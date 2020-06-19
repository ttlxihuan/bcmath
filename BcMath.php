<?php
//assert_options(ASSERT_ACTIVE, 1);
//assert_options(ASSERT_ACTIVE, 1);
/**
 * 高精度计算类
 */
class BcMath {

    /**
     * @var int 最大直接计算长度
     */
    private $maxSize = 0;

    /**
     * @var int 最大直接计算值
     */
    private $max = 0;

    /**
     * @var int 最大直接乘法长度
     */
    private $maxMulSize = 0;

    /**
     * @var int 最大直接乘法计算值
     */
    private $maxMul = 0;

    /**
     * @var int 超大乘法计算长度
     */
    private $hugeMulSize = 0;

    /**
     * @var int 最大直小数精度
     */
    private $maxScale = 100;

    /**
     * @var self 对象本身 
     */
    private static $self;

    /**
     * @var array 支持的运算
     */
    private $method = [
        '+' => 'bcadd',
        '-' => 'bcsub',
        '*' => 'bcmul',
        '/' => 'bcdiv',
        '%' => 'bcmod',
        '^' => 'bcpow',
        '~' => 'bcroot',
    ];

    /**
     * @var array 运算答优先级
     */
    private static $priority = [
        '+' => 1,
        '-' => 1,
        '*' => 2,
        '/' => 2,
        '%' => 2,
        '^' => 3,
        '~' => 3,
    ];

    /**
     * 初始化处理
     */
    protected function __construct() {
        $this->maxSize = strlen(PHP_INT_MAX) - 1;
        $this->max = pow(10, $this->maxSize);
        $this->maxMulSize = intval(($this->maxSize) / 2);
        $this->maxMul = pow(10, $this->maxMulSize);
        $this->hugeMulSize = $this->maxMulSize * 25;
    }

    /**
     * 获取对象本身并初始精度
     * @param int $scale    小数精度位数
     * @return \self
     */
    protected static function instance(int $scale): self {
        if (empty(static::$self)) {
            static::$self = new static();
        }
        static::$self->maxScale = $scale > 0 ? $scale : static::$self->maxScale;
        return static::$self;
    }

    /**
     * 解析公式并计算处理
     * 支持运算符： ＋、－、*、/、%、^
     * 支持小括号
     * @param string $formula   公式字符串
     * @param int $scale        小数精度位数
     * @return string           计算结果字符串
     * @throws \Exception
     */
    public static function counter(string $formula, int $scale = 10): string {
        if ($formula === '') {
            throw new \Exception('empty formula');
        }
        $length = strlen($formula);
        $self = static::instance($scale);
        $array = [];
        $queue = [];
        $number = '';
        $type = 'none';
        $float = '';
        for ($index = 0; $index <= $length; $index++) {
            if ($index < $length) {
                switch ($bit = $formula[$index]) {
                    case '(':
                        if ($number !== '') {
                            throw new \Exception('error syntax: ' . substr($formula, $index));
                        }
                        $queue[] = $array;
                        $array = [];
                        continue 2;
                    case ')':
                        if (count($queue) === 0) {
                            throw new \Exception('error syntax: ' . substr($formula, $index));
                        }
                        $type = 'ope';
                        break;
                    case "\r":
                    case "\n":
                    case "\t":
                    case ' ':
                        if ($number !== '') {
                            $type = 'ope';
                        }
                        continue 2;
                    default:
                        if ($type === 'none' || $type === 'val') {
                            $type = 'val';
                            switch ($bit) {
                                case '+':
                                case '-':
                                    if ($number !== '') {
                                        break;
                                    }
                                case '1':
                                case '2':
                                case '3':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                case '8':
                                case '9':
                                    if ($float === '0') {
                                        throw new \Exception('error number: ' . substr($formula, $index));
                                    }
                                    $number .= $bit;
                                    continue 3;
                                case '.':
                                    if ($float === '.') {
                                        throw new \Exception('error number: ' . substr($formula, $index));
                                    }
                                    $float = '.';
                                    if ($number === '') {
                                        $bit .= '0';
                                    }
                                    $number .= $bit;
                                    continue 3;
                                case '0':
                                    if ($number === '') {
                                        $float = '0';
                                    }
                                    $number .= $bit;
                                    continue 3;
                            }
                        }
                        switch ($bit) {
                            case '=':
                            case '>':
                            case '<':
                            case '-':
                            case '+':
                            case '*':
                            case '/':
                            case '%':
                            case '^':
                            case '~':
                                if ($type === 'ope' || ($type === 'val' && $number !== '' && $number !== '-' && $number !== '+')) {
                                    if ($number !== '') {
                                        $array[] = $number;
                                    }
                                    $array[] = $bit;
                                    $number = '';
                                    $float = '';
                                    $type = 'val';
                                    continue 3;
                                }
                        }
                        throw new \Exception('error formula: ' . substr($formula, $index));
                }
            }
            if ($number !== '') {
                $array[] = $number;
            }
            $size = count($array);
            $listen = [];
            for ($oper = 0; $size >= $oper; $oper += 2) {
                $curr = '';
                if ($size > $oper) {
                    array_push($listen, $array[$oper]);
                    if ($size > $oper + 1) {
                        $curr = $array[$oper + 1];
                    }
                }
                while (0 < $num = count($listen)) {
                    if ($num > 2 && ($curr === '' || static::$priority[$listen[$num - 2]] >= static::$priority[$curr])) {
                        $val2 = array_pop($listen);
                        $ope = array_pop($listen);
                        $val1 = array_pop($listen);
                        $listen[] = $self->accuracy($self->call($self->method[$ope], $val1, $val2), $scale);
                        if (end($listen) === null) {
                            throw new \Exception('Calculation error: ' . $ope);
                        }
                    } else {
                        if ($curr) {
                            array_push($listen, $curr);
                        }
                        break;
                    }
                }
            }
            if ($bit === ')') {
                $array = array_pop($queue) ?: [];
            }
            if (count($listen) === 1) {
                $number = $listen[0];
            } else {
                throw new \Exception('error syntax: ' . $bit);
            }
        }
        if (count($queue)) {
            throw new \Exception('error syntax: ( not end');
        }
        return $self->accuracy($number, $scale);
    }

    /**
     * 调用运算处理
     * @param string $method        运算方法
     * @param string $left_operand  左运算数
     * @param string $right_operand 右运算数
     * @return string
     */
    public function call(string $method, string $left_operand, string $right_operand): string {
        //数据校验
        $result = $this->$method($this->newNumber($left_operand), $this->newNumber($right_operand));
        if (is_array($result)) {
            return $this->restore($result);
        }
        return strval($result);
    }

    /**
     * 创建数据
     * @param string $operand   运算数
     * @return array
     * @throws \Exception
     */
    protected function newNumber(string $operand): array {
        if ($operand[0] === '+' || $operand[0] === '-') {
            $symbol = $operand[0];
            $operand = substr($operand, 1);
        } else {
            $symbol = '+';
        }
        if ($operand === '0') {
            $point = false;
            $symbol = '+';
        } else {
            $operand = $this->criterion($operand);
            $point = strpos($operand, '.');
            if ($point !== false) {
                $point = strlen($operand) - $point - 1;
                $operand = $this->criterion(str_replace('.', '', $operand, $count));
                if ($count !== 1) {
                    throw new \Exception('not number');
                }
            }
        }
        return ['symbol' => $symbol, 'operand' => $operand, 'size' => 0, 'point' => $point];
    }

    /**
     * 整理为标准数字,去掉前后不合理的零和小数标准化
     * @param string $operand   运算数
     * @return string
     */
    protected function criterion(string $operand): string {
        $length = strlen($operand);
        if ($length <= 1) {
            return $operand ?: '0';
        }
        for ($int = 0; $int < $length && $operand[$int] === '0'; $int++)
            ;
        $point = strpos($operand, '.');
        if (false !== $point) {
            for ($decimal = $length - 1; $decimal >= $point && $operand[$decimal] === '0'; $decimal--)
                ;
            if ($decimal < $length) {
                if ($operand[$decimal] !== '.') {
                    $decimal++;
                }
                $operand = substr($operand, 0, $decimal);
            }
            if ($point === $int) {
                $int--;
            }
        }
        if ($int > 0) {
            $operand = substr($operand, $int);
            if ($operand === '') {
                return '0';
            }
        }
        return $operand;
    }

    /**
     * 放大同等级小数
     * @param int $size             最大直接运算长度
     * @param array $left_array     左运算数
     * @param array $right_array    右运算数
     */
    protected function amplify(int $size, array &$left_array, array &$right_array) {
        $left_point = $left_array['point'];
        $right_point = $right_array['point'];
        if ($left_point !== $right_point) {
            $zero = str_repeat(0, abs($left_point - $right_point));
            if ($left_point > $right_point) {
                $right_operand = &$right_array['operand'];
                if ($right_operand !== '0') {
                    $right_operand .= $zero;
                    $right_array['point'] = $left_point;
                }
            } else {
                $left_operand = &$left_array['operand'];
                if ($left_operand !== '0') {
                    $left_operand .= $zero;
                    $left_array['point'] = $right_point;
                }
            }
        }
        foreach ([&$left_array, &$right_array] as &$operand) {
            if ($operand['size'] !== $size) {
                $items = [];
                $operand['size'] = $size;
                $number = $operand['operand'];
                for ($length = strlen($number); $length > 0; $length -= $size) {
                    $items[] = intval($length > $size ? substr($number, $length - $size, $size) : substr($number, 0, $length));
                }
                $operand['items'] = $items;
            }
        }
    }

    /**
     * 重建数据参数
     * @param array $operand
     * @return array
     */
    protected function rebuild(array $operand): array {
        $size = $operand['size'];
        if ($size > 0) {
            $number = '';
            foreach ($operand['items'] as $item) {
                settype($item, 'string');
                $number = str_repeat('0', $size - strlen($item)) . $item . $number;
            }
            if ($number[0] === '0') {
                $operand['size'] = 0;
            }
            $operand['operand'] = $this->criterion($number);
        }
        return $operand;
    }

    /**
     * 复原整数到指定精度小数
     * @param array $operand    拆分的数字
     * @param int $scale        小数位数
     * @return string
     */
    protected function restore(array $operand, int $scale = 0): string {
        $number = $operand['operand'];
        $scale += $operand['point'];
        if ($scale > 0 && $number !== '0') {
            $point = strpos($number, '.');
            $length = strlen($number);
            if ($point !== false) {
                $scale += $length - $point;
                $number = str_replace('.', '', $number);
                $length = strlen($number);
            }
            if ($length > $scale) {
                $pos = $length - $scale;
                $int = substr($number, 0, $pos);
                $decimal = substr($number, $pos);
            } else {
                $int = '0';
                if ($length > 0) {
                    $scale -= $length;
                    $decimal = $scale > 0 ? str_repeat('0', $scale) . $number : $number;
                } else {
                    $decimal = '';
                }
            }
            $number = $int;
            if ($decimal !== '') {
                $number .= '.' . $decimal;
            }
        }
        $symbol = $operand['symbol'] === '-' ? '-' : '';
        return $symbol . $this->criterion($number);
    }

    /**
     * 提取指定精度
     * @param string $number
     * @param int $scale
     * @return string
     */
    public function accuracy(string $number, int $scale): string {
        if (false !== $point = strpos($number, '.')) {
            $number = substr($number, 0, $point + $scale + 1);
        }
        return $this->criterion($number);
    }

    /**
     * 转换为最简分数
     * @param array $operand
     * @return array
     */
    protected function fraction(array $operand): array {
        $point = $operand['point'];
        if ($point === false) {
            return [$operand, $this->newNumber('1')];
        }
        $operand['point'] = false;
        $divisor = $operand;
        $dividend = $denominator = $this->newNumber('1' . str_repeat('0', $point));
        while ($dividend['operand'] !== '0') {
            $remainder = $this->bcmod($divisor, $dividend);
            $divisor = $dividend;
            $dividend = $remainder;
        }
        if ($divisor['operand'] === '1') {
            return [$operand, $denominator];
        }
        return [$this->bcdiv($operand, $divisor), $this->bcdiv($denominator, $divisor)];
    }

    /**
     * 比较两个数字大小
     * @param array $left_array  左运算数
     * @param array $right_array 右运算数
     * @return int
     */
    protected function bccomp(array $left_array, array $right_array): int {
        if ($left_array === $right_array) {
            return 0;
        }
        if ($left_array['symbol'] !== $right_array['symbol']) {
            return $left_array['symbol'] === '-' ? -1 : 1;
        } elseif ($left_array['symbol'] === '-') {
            $left_array['symbol'] = $right_array['symbol'] = '+';
            return $this->bccomp($right_array, $left_array);
        }
        $left_int = strlen($left_array['operand']) - $left_array['point'];
        $right_int = strlen($right_array['operand']) - $right_array['point'];
        if ($left_int > $right_int) {
            return 1;
        } elseif ($left_int > $right_int) {
            return -1;
        }
        $this->amplify($this->maxSize, $left_array, $right_array);
        $left_int_size = count($left = $left_array['items']);
        $right_int_size = count($right = $right_array['items']);
        if ($left_int_size > $right_int_size) {
            return 1;
        } elseif ($left_int_size < $right_int_size) {
            return -1;
        }
        for ($key = $left_int_size - 1; $key >= 0; $key--) {
            if ($left[$key] > $right[$key]) {
                return 1;
            } elseif ($left[$key] < $right[$key]) {
                return -1;
            }
        }
        return 0;
    }

    /**
     * 计算两个数字相加
     * @param array $left_array  左运算数
     * @param array $right_array 右运算数
     * @return array
     */
    protected function bcadd(array $left_array, array $right_array): array {
        if ($left_array['operand'] === '0') {
            return $right_array;
        }
        if ($right_array['operand'] === '0') {
            return $left_array;
        }
        if ($left_array['symbol'] !== $right_array['symbol']) {
            if ($left_array['symbol'] === '-') {
                $left_array['symbol'] = '+';
                return $this->bcsub($right_array, $left_array);
            } else {
                $right_array['symbol'] = '+';
                return $this->bcsub($left_array, $right_array);
            }
        }
        $this->amplify($this->maxSize, $left_array, $right_array);
        if (count($left_array['items']) < count($right_array['items'])) {
            list($right_array, $left_array) = [$left_array, $right_array];
        }
        $left_items = $left_array['items'];
        $right_items = $right_array['items'];
        foreach ($right_items as $key => $value) {
            $left_items[$key] += $value;
            if ($left_items[$key] >= $this->max) {
                $next = $key + 1;
                if (isset($left_items[$next])) {
                    $left_items[$next] += 1;
                } else {
                    $left_items[$next] = 1;
                }
                $left_items[$key] -= $this->max;
            }
        }
        //向上进位
        if (count($right_items) && isset($left_items[++$key])) {
            while ($left_items[$key] >= $this->max) {
                $next = $key + 1;
                if (isset($left_items[$next])) {
                    $left_items[$next] ++;
                } else {
                    $left_items[$next] = 1;
                }
                $left_items[$key++] -= $this->max;
            }
        }
        $left_array['items'] = $left_items;
        return $this->rebuild($left_array);
    }

    /**
     * 计算两个数字相减
     * @param array $left_array  左运算数
     * @param array $right_array 右运算数
     * @return string
     */
    protected function bcsub(array $left_array, array $right_array): array {
        if ($left_array['operand'] === '0') {
            if ($right_array['operand'] !== '0') {
                $right_array['symbol'] = $right_array['symbol'] === '-' ? '+' : '-';
            }
            return $right_array;
        }
        if ($right_array['operand'] === '0') {
            return $left_array;
        }
        if ($left_array['symbol'] !== $right_array['symbol']) {
            $right_array['symbol'] = $left_array['symbol'];
            return $this->bcadd($left_array, $right_array);
        }
        $this->amplify($this->maxSize, $left_array, $right_array);
        $comp = $this->bccomp($left_array, $right_array);
        if ($comp === 0) {
            return $this->newNumber('0');
        }
        if (($comp === 1 && $left_array['symbol'] === '-') || ($comp === -1 && $left_array['symbol'] !== '-')) {
            $right_array['symbol'] = $left_array['symbol'] === '-' ? '+' : '-';
            list($right_array, $left_array) = [$left_array, $right_array];
        }
        $left_items = $left_array['items'];
        foreach ($right_array['items'] as $key => $value) {
            if ($value > $left_items[$key]) {
                $next = $key + 1;
                $left_items[$next] -= 1;
                $left_items[$key] += $this->max;
            }
            $left_items[$key] -= $value;
        }
        //向上借位
        if (isset($left_items[++$key])) {
            while ($left_items[$key] < 0) {
                $left_items[$key++] += $this->max;
                $left_items[$key] --;
            }
        }
        $left_array['items'] = $left_items;
        return $this->rebuild($left_array);
    }

    /**
     * 计算两个数字相乘
     * @param array $left_array  左运算数
     * @param array $right_array 右运算数
     * @return array
     */
    protected function bcmul(array $left_array, array $right_array): array {
        $left_operand = $left_array['operand'];
        $right_operand = $right_array['operand'];
        $result = $this->newNumber('0');
        if ($left_operand === '0' || $right_operand === '0') {
            return $result;
        }
        $left_size = strlen($left_operand);
        $right_size = strlen($right_operand);
        $point = $left_array['point'] + $right_array['point'];
        if ($left_size < $this->hugeMulSize || $right_size < $this->hugeMulSize) {
            //禁止放大
            $left_array['point'] = $right_array['point'] = false;
            $this->amplify($this->maxMulSize, $left_array, $right_array);
            //直接计算
            $value = [];
            $result['size'] = $this->maxMulSize;
            $right_items = $right_array['items'];
            foreach ($left_array['items'] as $keyl => $left) {
                foreach ($right_items as $keyr => $right) {
                    $key = $keyl + $keyr;
                    $val = $left * $right;
                    if (isset($value[$key])) {
                        $value[$key] += $val;
                    } else {
                        $value[$key] = $val;
                    }
                    //处理进位
                    for (; $value[$key] >= $this->maxMul; $key++) {
                        //注意这里必需先取模，再减，否则运算时会导致精度丢失
                        $mod = $value[$key] % $this->maxMul;
                        $carry = intval(($value[$key] - $mod) / $this->maxMul);
                        $next = $key + 1;
                        if (isset($value[$next])) {
                            $value[$next] += $carry;
                        } else {
                            $value[$next] = $carry;
                        }
                        $value[$key] = $mod;
                    }
                }
            }
            $result['items'] = $value;
        } else {
            /*
             * 分治乘法计算
             *  Let u = u0 + u1*(b^n)
             *  Let v = v0 + v1*(b^n)
             *  Then uv = (B^2n+B^n)*u1*v1 + B^n*(u1-u0)*(v0-v1) + (B^n+1)*u0*v0
             */
            $m = intval((max($left_size, $right_size) + 1) / 2);
            if ($left_size <= $m) {
                $x1 = '0';
                $x0 = $left_operand;
            } else {
                $diff = $left_size - $m;
                $x1 = substr($left_operand, 0, $diff);
                $x0 = substr($left_operand, $diff);
            }
            if ($right_size <= $m) {
                $y1 = '0';
                $y0 = $right_operand;
            } else {
                $diff = $right_size - $m;
                $y1 = substr($right_operand, 0, $diff);
                $y0 = substr($right_operand, $diff);
            }
            $x1 = $this->newNumber($x1);
            $y1 = $this->newNumber($y1);
            if ($x1['operand'] !== '0' && $y1['operand'] !== '0') {
                $z2 = $this->bcmul($x1, $y1);
                $result = $this->skipAdd($z2, $this->skipAdd($z2, $result, $m * 2), $m);
            }
            $x0 = $this->newNumber($x0);
            $y0 = $this->newNumber($y0);
            $z1 = $this->bcmul($this->bcsub($x1, $x0), $this->bcsub($y0, $y1));
            $result = $this->skipAdd($z1, $result, $m);
            if ($x0['operand'] !== '0' && $y0['operand'] !== '0') {
                $z0 = $this->bcmul($x0, $y0);
                $result = $this->skipAdd($z0, $this->bcadd($z0, $result), $m);
            }
        }
        $result['point'] = $point;
        if ($result['point'] === 0) {
            $result['point'] = false;
        }
        if ($left_array['symbol'] !== $right_array['symbol']) {
            $result['symbol'] = '-';
        }
        return $this->rebuild($result);
    }

    /**
     * 跳相加
     * @param array $left_array  左运算数
     * @param array $right_array 右运算数
     * @param int $start 计算开始位置
     * @return array
     */
    protected function skipAdd(array $left_array, array $right_array, int $start): array {
        if ($left_array['symbol'] !== $right_array['symbol']) {
            $left_array['operand'] .= str_repeat('0', $start);
            $left_array['size'] = 0;
            return $this->bcadd($left_array, $right_array);
        } else {
            $right_operand = $right_array['operand'];
            $right_size = strlen($right_operand);
            if ($start >= $right_size) {
                $left_array['operand'] .= str_repeat('0', $start - $right_size) . $right_operand;
            } else {
                $num = $right_size - $start;
                $result = $this->bcadd($left_array, $this->newNumber(substr($right_operand, 0, $num)));
                $left_array['operand'] = $result['operand'] . substr($right_operand, $num);
            }
            $left_array['size'] = 0;
        }
        return $left_array;
    }

    /**
     * 计算两个数字相除
     * @param array $left_array  左运算数
     * @param array $right_array 右运算数
     * @param int $scale 小数精度位数
     * @return array
     */
    protected function bcdiv(array $left_array, array $right_array, int $scale = null): array {
        $this->amplify($this->maxSize, $left_array, $right_array);
        $mod = $this->bcmod($left_array, $right_array);
        // 提取商
        $value = $mod['divided'];
        if ($left_array['symbol'] !== $right_array['symbol']) {
            $value = '-' . $value;
        }
        // 处理余数
        if ($mod['operand'] !== '0') {
            if (is_null($scale)) {
                $scale = $this->maxScale;
            }
            $mod['point'] = $right_array['point'] = false;
            $right_size = strlen($right_array['operand']);
            $mod_size = strlen($mod['operand']);
            $mod['operand'] .= str_repeat('0', $scale);
            $result = $this->bcmod($mod, $right_array);
            $decimal = $result['divided'];
            if ($decimal !== '0') {
                //计算最大填充零数
                $zero_size = $right_size - $mod_size;
                if ($zero_size > 0) {
                    $right_array['symbol'] = '+';
                    if ($this->bccomp($this->newNumber(substr($mod['operand'], 0, $right_size)), $right_array) >= 0) {
                        $zero_size--;
                    }
                    if ($zero_size > 0) {
                        $decimal = str_repeat('0', $zero_size) . $decimal;
                    }
                }
                $value .= '.' . $decimal;
            }
        }
        return $this->newNumber($value);
    }

    /**
     * 除法计算
     * @param array $left_array
     * @param array $right_array
     * @return array
     */
    protected function bcmod(array $left_array, array $right_array): array {
        $symbol = $left_array['symbol'];
        $result = $this->newNumber('0');
        $this->amplify($this->maxSize, $left_array, $right_array);
        $left_operand = $left_array['operand'];
        $right_operand = $right_array['operand'];
        //某运算数为0不需要计算
        if ($left_operand === '0' || $right_operand === '0') {
            $result['divided'] = '0';
            return $result;
        }
        //两个运算数相等，直接为 1
        if ($left_operand === $right_operand) {
            $result['divided'] = '1';
            $result['symbol'] = $symbol;
            return $result;
        }
        $left_size = strlen($left_operand);
        $right_size = strlen($right_operand);
        //左运算数小于右运算数，无需计算
        if ($left_size < $right_size) {
            $left_array['divided'] = '0';
            return $left_array;
        }
        //两运算数长度都在最大计算长度之内
        if ($left_size <= $this->maxSize) {
            $operand = strval($left_operand % $right_operand);
            $divided = strval(intval(($left_operand - $operand) / $right_operand));
        } else {
            $operand = "";
            $divided = "";
            $end = 0;
            $step = $this->maxSize - 1;
            //右运算数长度在最大计算长度之内
            if ($right_size < $this->maxSize) {
                settype($right_operand, 'int');
                $operand_size = 0;
                do {
                    $start = $end;
                    $diff = $step - $operand_size;
                    $end += max($diff, 1);
                    if ($end > $left_size) {
                        $diff -= $end - $left_size;
                        $end = $left_size;
                    }
                    $operand .= substr($left_operand, $start, $end - $start);
                    $left = intval($operand);
                    if ($divided !== '') {
                        $step_diff = strlen($operand) - strlen($left);
                        if ($step_diff > 0 && strlen($operand) >= $right_size && $left > 0) {
                            $step_diff += $right_size;
                        } elseif ($step_diff == 0) {
                            $step_diff = min($right_size - $operand_size, $diff);
                        }
                        if ($left >= $right_operand && substr($left, 0, $right_size) >= $right_operand) {
                            $step_diff--;
                        }
                        if ($step_diff > 0) {
                            $divided .= str_repeat('0', $step_diff);
                        }
                    }
                    if ($left < $right_operand) {
                        $operand = strval($left);
                    } else {
                        $operand = strval($left % $right_operand);
                        $divided .= strval(($left - $operand) / $right_operand);
                    }
                    if ($operand === '0') {
                        $operand = '';
                    }
                    $operand_size = strlen($operand);
                } while ($end + 1 <= $left_size);
            } else {
                //两运算数长度都在最大计算长度之外
                //临时运算数据
                $right_tmp = $this->newNumber($right_operand);
                $sub_number = [0, $right_tmp];
                //运算数拆分
                $right = intval(substr($right_operand, 0, $step));
                do {
                    $start = $end;
                    $diff = $right_size - strlen($operand);
                    $end += max($diff, 1);
                    if ($end > $left_size) {
                        $diff -= $end - $left_size;
                        $end = $left_size;
                    }
                    $operand .= substr($left_operand, $start, $end - $start);
                    if ($diff > 1 && $divided !== '') {
                        $divided .= str_repeat('0', $diff - 1);
                    }
                    if ($right_size > strlen($operand) || ($right_size == strlen($operand) && $right_operand[0] > $operand[0])) {
                        $operand = $this->criterion($operand);
                        $multiple = 0;
                    } else {
                        $left = intval(substr($operand, 0, $diff == 0 ? $this->maxSize : $step));
                        //取出大概商
                        $multiple = intval($left / $right);
                        if ($multiple > 0) {
                            if ($multiple > 9) {
                                $multiple = 9;
                            }
                            $operand_tmp = $this->newNumber($operand);
                            if (empty($sub_number[$multiple])) {
                                $sub_number[$multiple] = $this->bcmul($this->newNumber(strval($multiple)), $right_tmp);
                            }
                            //商不可以超出
                            if ($this->bccomp($sub_number[$multiple], $operand_tmp) > 0) {
                                $multiple--;
                                if (empty($sub_number[$multiple])) {
                                    $sub_number[$multiple] = $this->bcmul($this->newNumber(strval($multiple)), $right_tmp);
                                }
                            }
                            $operand = $this->bcsub($operand_tmp, $sub_number[$multiple])['operand'];
                        }
                    }
                    $divided .= strval($multiple);
                } while ($end + 1 <= $left_size);
                $divided = $this->criterion($divided);
            }
        }
        $result['operand'] = $operand;
        $result['divided'] = $divided;
        $result['point'] = $left_array['point'];
        if ($operand != '0') {
            $result['symbol'] = $symbol;
        }
        return $result;
    }

    /**
     * 计算两个数字相幂
     * @param array $left_array  左运算数
     * @param array $right_array 右运算数
     * @return array
     */
    protected function bcpow(array $left_array, array $right_array): array {
        $right_point = $right_array['point'];
        $right_symbol = $right_array['symbol'];
        if ($right_array['operand'] === '0') {
            return $this->newNumber('1');
        }
        if ($left_array['operand'] === '0') {
//            if ($right_symbol === '-') {
//                throw new \Exception('0 没有负幂');
//            }
            return $this->newNumber('0');
        }
        $right_array['symbol'] = '+';
        //小数需要化简
        if ($right_point !== false) {
            list($right_array, $right_denominator) = $this->fraction($right_array);
        }
        $symbol = $left_array['symbol'];
        if ($symbol === '-' && $this->bcmod($right_array, $this->newNumber('2'))['operand'] === '0') {
            $left_array['symbol'] = '+';
        }
        //计算出最大幂
        $one_array = $this->newNumber('1');
        $array = [];
        $pows = [];
        $array[] = $pow_current = $left_array;
        $pows[] = $pow_array = $one_array;
        while (true) {
            $new_pow = $this->bcadd($pow_array, $pow_array);
            if ($this->bccomp($new_pow, $right_array) <= 0) {
                $array[] = $pow_current = $this->bcmul($pow_current, $pow_current);
                $pows[] = $new_pow;
                $pow_array = $new_pow;
            } else {
                break;
            }
        }
        //补充缺少幂
        $count = count($pows);
        while (true) {
            $right_array = $this->bcsub($right_array, $pow_array);
            if ($right_array['operand'] === '0') {
                break;
            }
            for ($key = (strlen($right_array['operand']) - 1) * 3; $key < $count && $this->bccomp($pows[$key], $right_array) <= 0; $key++)
                ;
            $pow_array = $pows[--$key];
            $pow_current = $this->bcmul($pow_current, $array[$key]);
        }
        //小数幂
        // a^(b/c) = a^b~c
        if ($right_point !== false) {
            $pow_current = $this->bcroot($pow_current, $right_denominator);
        }
        //负数需要进行反转
        if ($right_symbol === '-') {
            return $this->bcdiv($one_array, $pow_current);
        }
        return $pow_current;
    }

    /**
     * 计算一个数的方根值
     * @param array $left_array  左运算数
     * @param array $right_array 右运算数
     * @return array
     */
    protected function bcroot(array $left_array, array $right_array): array {
        $left_operand = $left_array['operand'];
        $right_operand = $right_array['operand'];
        $right_symbol = $right_array['symbol'];
        $right_point = $right_array['point'];
        if ($right_operand === '1' || $left_operand === '1') {
            return $left_array;
        }
        if ($left_operand === '0') {
            return $left_array;
        }
        if ($right_operand === '0') {
            throw new \Exception('不能取任何数的0次方根');
        }
        $right_array['symbol'] = '+';
        if ($left_array['symbol'] === '-') {
            $mod = $this->bcmod($right_array, $this->newNumber('2'))['operand'];
            if ($mod === '0') {
                throw new \Exception('双数幂开不出负数方根');
            }
            $symbol = '-';
            $left_array['symbol'] = '+';
        } else {
            $symbol = '+';
        }
        //小数需要化简
        if ($right_point !== false) {
            list($right_array, $right_denominator) = $this->fraction($right_array);
            // 小数方根，计算公式
            // a~(b/c) = a^c~b
            $left_array = $this->bcpow($left_array, $right_denominator);
        }
        $one_array = $this->newNumber('1');
        // 计算出初始根近似值
        $diff = $this->bcsub($right_array, $one_array);
        //大概计算根是几位数整数
        $int_size = strlen($left_operand) - $left_array['point'];
        $mod = $this->bcmod($this->newNumber((string) $int_size), $right_array);
        $length = intval($mod['divided']);
        if ($mod['operand'] !== '0') {
            $length++;
        }
        // 任意数开任意次方的公式：牛顿迭代法，循环直到接近
        // a~b = (c*(b-1)+a/c^(b-1))/b;
        $init_number = $this->newNumber(substr($left_operand, 0, min($length, strlen($left_operand))));
        $newroot = $this->bcdiv($this->bcadd($this->bcmul($init_number, $diff), $this->bcdiv($left_array, $this->bcpow($init_number, $diff))), $right_array);
        do {
            $root = $newroot;
            $newroot = $this->bcdiv($this->bcadd($this->bcmul($root, $diff), $this->bcdiv($left_array, $this->bcpow($root, $diff))), $right_array);
        } while ($root['operand'] !== $newroot['operand']);
        if ($right_symbol === '-') {
            $root = $this->bcdiv($one_array, $root);
        }
        $root['symbol'] = $symbol;
        return $root;
    }

}
