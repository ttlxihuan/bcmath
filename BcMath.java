/*
 * 高精度计算
 */
package code.math.com;

/**
 * 高精度计算类
 */
public class BcMath {

    /**
     * 最大直接计算长度
     */
    private static byte maxSize = 18;

    /**
     * 最大直接计算值
     */
    private static long max = 1000000000000000000L;

    /**
     * 最大直接乘法长度
     */
    private static byte maxMulSize = 9;

    /**
     * 最大直接乘法计算值
     */
    private static int maxMul = 1000000000;

    /**
     * 超大乘法计算长度
     */
    private static short hugeMulSize = 9 * 25;

    /**
     * 最大直小数精度
     */
    private static int maxScale = 100;

    /**
     * 支持的运算
     */
    private static String[] methods = {
        "bcadd",
        "bcsub",
        "bcmul",
        "bcdiv",
        "bcmod",
        "bcpow",
        "bcroot"
    };

    /**
     * 运算符优先级
     */
    private static byte[] prioritys = {
        1, 1, 2, 2, 2, 3, 3
    };
    /**
     * 运算符
     */
    private static String operators = "+-*/%^~";

    /**
     * 解析公式并计算处理 支持运算符： ＋、－、*、/、%、^ 支持小括号
     *
     * @param formula 公式字符串
     * @param scale 小数精度位数
     * @return String 计算结果字符串
     */
    public static String counter(String formula, int scale) throws Exception {
        if (formula.isEmpty()) {
            return "0";
        }
        int length = formula.length(),
                array_size = 1,
                queue_size = 1,
                pos = 0,
                index,
                next,
                oper,
                array_index = 0,
                queue_index = 0,
                listen_index;
        //获取长度
        while (0 <= (pos = formula.indexOf("(", pos))) {
            queue_size++;
            pos++;
        }
        for (index = operators.length() - 1; index >= 0; index--) {
            pos = 0;
            while (0 <= (pos = formula.indexOf(operators.charAt(index), pos))) {
                array_size++;
                pos++;
            }
        }
        array_size *= 2;
        int[] array_indexs = new int[queue_size];
        String[] array = new String[array_size], listen;
        String[][] queue = new String[queue_size][array_size];
        String number = "",
                type = "none",
                right,
                left,
                operator;
        char has_float = '?',
                bit;
        byte priority;
        for (index = 0; index <= length; index++) {
            if (index < length) {
                switch (bit = formula.charAt(index)) {
                    case '(':
                        if (!number.isEmpty()) {
                            throw new Exception("error syntax: " + formula.substring(index));
                        }
                        array_indexs[queue_index] = array_index;
                        queue[queue_index++] = array;
                        array_index = 0;
                        array = new String[array_size];
                        continue;
                    case ')':
                        if (queue_index == 0) {
                            throw new Exception("error syntax: " + formula.substring(index));
                        }
                        type = "ope";
                        break;
                    case '\r':
                    case '\n':
                    case '\t':
                    case ' ':
                        if (!number.isEmpty()) {
                            type = "ope";
                        }
                        continue;
                    default:
                        if (type.equals("none") || type.equals("val")) {
                            type = "val";
                            switch (bit) {
                                case '+':
                                case '-':
                                    if (!number.isEmpty()) {
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
                                    if (has_float == '0') {
                                        throw new Exception("error syntax: " + formula.substring(index));
                                    }
                                    number += bit;
                                    continue;
                                case '.':
                                    if (has_float == '.') {
                                        throw new Exception("error syntax: " + formula.substring(index));
                                    }
                                    has_float = '.';
                                    if (number.isEmpty()) {
                                        number += '0';
                                    }
                                    number += bit;
                                    continue;
                                case '0':
                                    if (number.isEmpty()) {
                                        has_float = '0';
                                    }
                                    number += bit;
                                    continue;
                            }
                        }
                        switch (bit) {
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
                                if (type.equals("ope") || (type.equals("val") && !number.isEmpty() && !number.equals("-") && !number.equals("+"))) {
                                    if (!number.isEmpty()) {
                                        array[array_index++] = number;
                                    }
                                    array[array_index++] = "" + bit;
                                    number = "";
                                    has_float = '?';
                                    type = "val";
                                    continue;
                                }
                        }
                        throw new Exception("error syntax: " + formula.substring(index));
                }
            } else {
                bit = '?';
            }
            if (!number.isEmpty()) {
                array[array_index++] = number;
            }
            if (array_index % 2 != 1) {
                throw new Exception("error syntax: " + formula.substring(index));
            }
            listen = new String[array_index];
            listen_index = 0;
            listen[listen_index++] = array[0];
            for (oper = 1; array_index > oper; oper++) {
                listen[listen_index++] = array[oper];
                listen[listen_index++] = array[++oper];
                next = oper + 1;
                if (array_index > next) {
                    priority = prioritys[operators.indexOf(array[next])];
                } else {
                    priority = 0;
                }
                while (listen_index > 2) {
                    if (prioritys[operators.indexOf(listen[listen_index - 2])] < priority) {
                        break;
                    }
                    right = listen[--listen_index];
                    operator = listen[--listen_index];
                    left = listen[--listen_index];
                    listen[listen_index++] = accuracy(call(methods[operators.indexOf(operator)], left, right), scale);
                }
            }
            if (bit == ')') {
                array = queue[--queue_index];
                array_index = array_indexs[queue_index];
            }
            if (listen_index == 1) {
                number = listen[0];
            } else {
                throw new Exception("error syntax: " + bit);
            }
        }
        if (queue_index > 0) {
            throw new Exception("error syntax: ( not end");
        }
        return accuracy(number, scale);
    }

    /**
     * 调用运算处理
     *
     * @param method 运算方法
     * @param left_operand 左运算数
     * @param right_operand 右运算数
     * @return String
     */
    public static String call(String method, String left_operand, String right_operand) throws Exception {
        //数据校验
        Number result = new Number("0");
        Number left = Number.create(left_operand);
        Number right = Number.create(right_operand);
        switch (method) {
            case "bcadd":
                result = bcadd(left, right);
                break;
            case "bcsub":
                result = bcsub(left, right);
                break;
            case "bcmul":
                result = bcmul(left, right);
                break;
            case "bcdiv":
                result = bcdiv(left, right, maxScale);
                break;
            case "bcmod":
                result = bcmod(left, right);
                break;
            case "bcpow":
                result = bcpow(left, right);
                break;
            case "bcroot":
                result = bcroot(left, right);
                break;
        }
        return restore(result);
    }

    /**
     * 整理为标准数字,去掉前后不合理的零和小数标准化
     *
     * @param operand 运算数
     * @return string
     */
    protected static String criterion(String operand) {
        int length = operand.length();
        if (length < 1) {
            return "0";
        }
        int int_num = 0, point;
        for (; int_num < length && operand.charAt(int_num) == '0'; int_num++)
            ;
        point = operand.indexOf('.');
        if (-1 != point) {
            int decimal = length - 1;
            for (; decimal >= point && operand.charAt(decimal) == '0'; decimal--)
                ;
            if (decimal < length) {
                if (operand.charAt(decimal) != '.') {
                    decimal++;
                }
                operand = operand.substring(0, decimal);
            }
            if (point == int_num) {
                int_num--;
            }
        }
        if (int_num > 0) {
            operand = operand.substring(int_num);
            if (operand.isEmpty()) {
                return "0";
            }
        }
        return operand;
    }

    /**
     * 复制多个零
     *
     * @param num
     * @return String
     */
    public static String zero_repeat(int num) {
        if (num > 0) {
            StringBuilder string = new StringBuilder();
            string.append('0');
            while (num >= string.length() * 2) {
                string.append(string);
            }
            if (num > string.length()) {
                string.append(string.substring(0, num - string.length()));
            }
            return string.toString();
        }
        return "";
    }

    /**
     * 重建数据参数
     *
     * @param operand
     * @return Number
     */
    protected static Number rebuild(Number operand) {
        int size = operand.size;
        if (size > 0) {
            StringBuilder number = new StringBuilder();
            String item;
            for (int num = operand.items_index - 1; num >= 0; num--) {
                item = String.valueOf(operand.items[num]);
                number.append(zero_repeat(size - item.length()));
                number.append(operand.items[num]);
            }
            if (number.length() == 0) {
                return new Number('0');
            }
            if (number.charAt(0) == '0') {
                operand.size = 0;
            }
            operand.operand = criterion(number.toString());
        }
        return operand;
    }

    /**
     * 复原整数到指定精度小数
     *
     * @param operand 拆分的数字
     * @return String
     */
    protected static String restore(Number operand) {
        return restore(operand, 0);
    }

    /**
     * 复原整数到指定精度小数
     *
     * @param operand 拆分的数字
     * @param scale 小数位数
     * @return String
     */
    protected static String restore(Number operand, int scale) {
        String number = operand.operand;
        scale += operand.point();
        if (scale > 0 && !number.equals("0")) {
            int point = number.indexOf('.'),
                    length = number.length();
            if (point != -1) {
                scale += length - point;
                number = number.replace(".", "");
                length = number.length();
            }
            String int_str, decimal;
            if (length > scale) {
                int pos = length - scale;
                int_str = number.substring(0, pos);
                decimal = number.substring(pos);
            } else {
                int_str = "0";
                if (length > 0) {
                    scale -= length;
                    decimal = scale > 0 ? zero_repeat(scale) + number : number;
                } else {
                    decimal = "";
                }
            }
            number = int_str;
            if (!decimal.isEmpty()) {
                number += '.' + decimal;
            }
        }
        return (operand.symbol == '-' ? operand.symbol : "") + criterion(number);
    }

    /**
     * 提取指定精度
     *
     * @param number
     * @param scale
     * @return String
     */
    protected static String accuracy(String number, int scale) {
        int point = number.indexOf('.');
        if (-1 != point) {
            number = number.substring(0, Math.min(point + scale + 1, number.length()));
        }
        return criterion(number);
    }

    /**
     * 放大同等级小数
     *
     * @param size 最大直接运算长度
     * @param left_number 左运算数
     * @param right_number 右运算数
     */
    protected static void amplify(int size, Number left_number, Number right_number) {
        int left_point = left_number.point();
        int right_point = right_number.point();
        if (left_point != right_point) {
            String zero = zero_repeat(Math.abs(left_point - right_point));
            if (left_point > right_point) {
                if (!right_number.operand.equals("0")) {
                    right_number.operand += zero;
                    right_number.point = left_point;
                }
            } else {
                if (!left_number.operand.equals("0")) {
                    left_number.operand += zero;
                    left_number.point = right_point;
                }
            }
        }
        Number[] numbers = {left_number, right_number};
        int index, length;
        long[] items;
        String number;
        for (int num = numbers.length - 1; num >= 0; num--) {
            if (numbers[num].size != size) {
                numbers[num].size = size;
                number = numbers[num].operand;
                index = 0;
                length = number.length();
                items = new long[length / size + 1];
                for (; length > 0; length -= size) {
                    items[index++] = Long.parseLong(length > size
                            ? number.substring(length - size, length)
                            : number.substring(0, length));
                }
                numbers[num].items_index = index;
                numbers[num].items = items;
            }
        }
    }

    /**
     * 转换为最简分数
     *
     * @param operand
     * @return Number[]
     */
    protected static Number[] fraction(Number operand) {
        if (operand.point == -1) {
            Number[] numbers = {operand, new Number('1')};
            return numbers;
        }
        int point = operand.point;
        operand.point = -1;
        Number remainder, dividend, divisor = operand, denominator = new Number('1' + zero_repeat(point));
        dividend = denominator;
        while (!dividend.operand.equals("0")) {
            remainder = bcmod(divisor, dividend);
            divisor = dividend;
            dividend = remainder;
        }
        if (divisor.operand.equals("1")) {
            Number[] numbers = {operand, denominator};
            return numbers;
        }
        Number[] numbers = {bcdiv(operand, divisor, maxScale), bcdiv(denominator, divisor, maxScale)};
        return numbers;
    }

    /**
     * 比较两个数字大小
     *
     * @param left_number 左运算数
     * @param right_number 右运算数
     * @return int
     */
    protected static int bccomp(Number left_number, Number right_number) {
        if (left_number.equals(right_number)) {
            return 0;
        }
        if (left_number.symbol != right_number.symbol) {
            return left_number.symbol == '-' ? -1 : 1;
        } else if (left_number.symbol == '-') {
            left_number = left_number.copy();
            right_number = right_number.copy();
            left_number.symbol = right_number.symbol = '+';
            return bccomp(right_number, left_number);
        }
        int left_int = left_number.operand.length() - left_number.point,
                right_int = right_number.operand.length() - right_number.point;
        if (left_int > right_int) {
            return 1;
        } else if (left_int > right_int) {
            return -1;
        }
        amplify(maxSize, left_number, right_number);
        long[] left_items = left_number.items,
                right_items = right_number.items;
        int left_items_index = left_number.items_index,
                right_items_index = right_number.items_index;
        if (left_items_index > right_items_index) {
            return 1;
        } else if (left_items_index < right_items_index) {
            return -1;
        }
        for (int key = left_items_index - 1; key >= 0; key--) {
            if (left_items[key] > right_items[key]) {
                return 1;
            } else if (left_items[key] < right_items[key]) {
                return -1;
            }
        }
        return 0;
    }

    /**
     * 计算两个数字相加
     *
     * @param left_number 左运算数
     * @param right_number 右运算数
     * @return Number
     */
    protected static Number bcadd(Number left_number, Number right_number) {
        if (left_number.operand.equals("0")) {
            return right_number;
        }
        if (right_number.operand.equals("0")) {
            return left_number;
        }
        if (left_number.symbol != right_number.symbol) {
            if (left_number.symbol == '-') {
                left_number.symbol = '+';
                return bcsub(right_number, left_number);
            } else {
                right_number.symbol = '+';
                return bcsub(left_number, right_number);
            }
        }
        amplify(maxSize, left_number, right_number);
        if (left_number.items_index < right_number.items_index) {
            Number number = left_number;
            left_number = right_number;
            right_number = number;
        }
        int left_items_index = left_number.items_index,
                right_items_index = right_number.items_index,
                key = 0, next;
        long[] left_items = left_number.items,
                right_items = right_number.items,
                items = new long[left_items_index + 1];
        items[0] = 0;
        for (; key < right_items_index; key++) {
            items[key] += left_items[key] + right_items[key];
            next = key + 1;
            if (items[key] >= max) {
                items[next] = 1;
                items[key] -= max;
            } else {
                items[next] = 0;
            }
        }
        //向上进位
        for (; key < left_items_index; key++) {
            items[key] += left_items[key];
            next = key + 1;
            if (items[key] >= max) {
                items[next] = 1;
                items[key] -= max;
            } else {
                items[next] = 0;
            }
        }
        left_number.items = items;
        left_number.items_index = key + 1;
        return rebuild(left_number);
    }

    /**
     * 计算两个数字相减
     *
     * @param left_number 左运算数
     * @param right_number 右运算数
     * @return Number
     */
    protected static Number bcsub(Number left_number, Number right_number) {
        if (left_number.operand.equals("0")) {
            if (!right_number.operand.equals("0")) {
                right_number.symbol = right_number.symbol == '-' ? '+' : '-';
            }
            return right_number;
        }
        if (right_number.operand.equals("0")) {
            return left_number;
        }
        if (left_number.symbol != right_number.symbol) {
            right_number.symbol = left_number.symbol;
            return bcadd(left_number, right_number);
        }
        amplify(maxSize, left_number, right_number);
        int comp = bccomp(left_number, right_number);
        if (comp == 0) {
            return new Number('0');
        }
        if ((comp == 1 && left_number.symbol == '-') || (comp == -1 && left_number.symbol != '-')) {
            right_number.symbol = left_number.symbol == '-' ? '+' : '-';
            Number number = left_number;
            left_number = right_number;
            right_number = number;
        }
        int left_items_index = left_number.items_index,
                right_items_index = right_number.items_index,
                key = 0, next;
        long[] left_items = left_number.items,
                right_items = right_number.items;

        for (; key < right_items_index; key++) {
            if (right_items[key] > left_items[key]) {
                next = key + 1;
                left_items[next] -= 1;
                left_items[key] += max;
            }
            left_items[key] -= right_items[key];
        }
        //向上借位
        if (left_items_index > key) {
            while (left_items[key] < 0) {
                left_items[key++] += max;
                left_items[key]--;
            }
        }
        return rebuild(left_number);
    }

    /**
     * 计算两个数字相乘
     *
     * @param left_number 左运算数
     * @param right_number 右运算数
     * @return Number
     */
    protected static Number bcmul(Number left_number, Number right_number) {
        String left_operand = left_number.operand,
                right_operand = right_number.operand;
        Number result = new Number('0');
        if (left_operand.equals("0") || right_operand.equals("0")) {
            return result;
        }
        int left_size = left_operand.length(),
                right_size = right_operand.length(),
                point = left_number.point() + right_number.point();
        if (left_size < hugeMulSize || right_size < hugeMulSize) {
            //禁止放大
            left_number.point = right_number.point = -1;
            amplify(maxMulSize, left_number, right_number);
            //直接计算
            result.size = maxMulSize;
            int left_items_index = left_number.items_index,
                    right_items_index = right_number.items_index,
                    keyl = 0, keyr, key = 0, next;
            long left, right, mod, carry;
            long[] items = new long[left_items_index + right_items_index],
                    right_items = right_number.items;
            for (; keyl < left_items_index; keyl++) {
                left = left_number.items[keyl];
                for (keyr = 0; keyr < right_items_index; keyr++) {
                    right = right_items[keyr];
                    key = keyl + keyr;
                    items[key] += left * right;
                    //处理进位
                    for (; items[key] >= maxMul; key++) {
                        //注意这里必需先取模，再减，否则运算时会导致精度丢失
                        mod = items[key] % maxMul;
                        carry = (items[key] - mod) / maxMul;
                        next = key + 1;
                        items[next] += carry;
                        items[key] = mod;
                    }
                }
            }
            result.items = items;
            result.items_index = key + 1;
        } else {
            /*
             * 分治乘法计算
             *  Let u = u0 + u1*(b^n)
             *  Let v = v0 + v1*(b^n)
             *  Then uv = (B^2n+B^n)*u1*v1 + B^n*(u1-u0)*(v0-v1) + (B^n+1)*u0*v0
             */
            int m = (int) ((Math.max(left_size, right_size) + 1) / 2), diff;
            Number x0, x1, y0, y1, z0, z1, z2;
            if (left_size <= m) {
                x1 = new Number('0');
                x0 = new Number(left_operand);
            } else {
                diff = left_size - m;
                x1 = new Number(left_operand.substring(0, diff));
                x0 = new Number(left_operand.substring(diff));
            }
            if (right_size <= m) {
                y1 = new Number('0');
                y0 = new Number(right_operand);
            } else {
                diff = right_size - m;
                y1 = new Number(right_operand.substring(0, diff));
                y0 = new Number(right_operand.substring(diff));
            }
            if (!x1.operand.equals("0") && !y1.operand.equals("0")) {
                z2 = bcmul(x1, y1);
                result = skipAdd(z2.copy(), result, m * 2);
                result = skipAdd(z2, result, m);
            }
            z1 = bcmul(bcsub(x1, x0.copy()), bcsub(y0.copy(), y1));
            result = skipAdd(z1, result, m);
            if (!x0.operand.equals("0") && !y0.operand.equals("0")) {
                z0 = bcmul(x0, y0);
                result = bcadd(z0.copy(), result);
                result = skipAdd(z0, result, m);
            }
        }
        result.point = point;
        if (result.point == 0) {
            result.point = -1;
        }
        if (left_number.symbol != right_number.symbol) {
            result.symbol = '-';
        }
        return rebuild(result);
    }

    /**
     * 跳相加
     *
     * @param left_number 左运算数
     * @param right_number 右运算数
     * @param start 计算开始位置
     * @return array
     */
    protected static Number skipAdd(Number left_number, Number right_number, int start) {
        if (left_number.symbol != right_number.symbol) {
            left_number.operand += zero_repeat(start);
            left_number.size = 0;
            return bcadd(left_number, right_number);
        } else {
            String right_operand = right_number.operand;
            int right_size = right_operand.length();
            if (start >= right_size) {
                left_number.operand += zero_repeat(start - right_size) + right_operand;
            } else {
                int num = right_size - start;
                Number result = bcadd(left_number, new Number(right_operand.substring(0, num)));
                left_number.operand = result.operand + right_operand.substring(num);
            }
            left_number.size = 0;
        }
        return left_number;
    }

    /**
     * 计算两个数字相除
     *
     * @param left_number 左运算数
     * @param right_number 右运算数
     * @param scale 小数精度位数
     * @return array
     */
    protected static Number bcdiv(Number left_number, Number right_number, int scale) {
        amplify(maxSize, left_number, right_number);
        Number mod = bcmod(left_number, right_number);
        // 提取商
        String value = mod.divided;
        if (left_number.symbol != right_number.symbol) {
            value = '-' + value;
        }
        // 处理余数
        if (!mod.operand.equals("0")) {
            mod.point = right_number.point = -1;
            int right_size = right_number.operand.length(),
                    mod_size = mod.operand.length();
            mod.operand += zero_repeat(scale);
            Number result = bcmod(mod, right_number);
            String decimal = result.divided;
            if (!decimal.equals("0")) {
                //计算最大填充零数
                int zero_size = right_size - mod_size;
                if (zero_size > 0) {
                    right_number.symbol = '+';
                    //判断第一位是否为0
                    if (bccomp(new Number(mod.operand.substring(0, right_size)), right_number) >= 0) {
                        zero_size--;
                    }
                    if (zero_size > 0) {
                        decimal = zero_repeat(zero_size) + decimal;
                    }
                }
                value += '.' + decimal;
            }
        }
        return new Number(value);
    }

    /**
     * 计算两个数字相除的余数
     *
     * @param left_number 左运算数
     * @param right_number 右运算数
     * @return Number
     */
    protected static Number bcmod(Number left_number, Number right_number) {
        char symbol = left_number.symbol;
        Number result = new Number('0');
        amplify(maxSize, left_number, right_number);
        String left_operand = left_number.operand,
                right_operand = right_number.operand;
        //某运算数为0不需要计算
        if (left_operand.equals("0") || right_operand.equals("0")) {
            result.divided = "0";
            return result;
        }
        //两个运算数相等，直接为 1
        if (left_operand.equals(right_operand)) {
            result.divided = "1";
            result.symbol = symbol;
            return result;
        }
        int left_size = left_operand.length(),
                right_size = right_operand.length();
        //左运算数小于右运算数，无需计算
        if (left_size < right_size) {
            left_number.divided = "0";
            return left_number;
        }
        long left, right;
        String divided, operand = "";
        //两运算数长度都在最大计算长度之内
        if (left_size <= maxSize) {
            left = Long.parseLong(left_operand);
            right = Long.parseLong(right_operand);
            operand = String.valueOf(left % right);
            divided = String.valueOf(left / right);
        } else {
            int start, end = 0, step = maxSize - 1, diff;
            //右运算数长度在最大计算长度之内
            StringBuilder divided_tmp = new StringBuilder();
            if (right_size < maxSize) {
                int step_diff, operand_size = 0;
                right = Long.parseLong(right_operand);
                do {
                    start = end;
                    diff = step - operand.length();
                    end += Math.max(diff, 1);
                    if (end > left_size) {
                        diff -= end - left_size;
                        end = left_size;
                    }
                    operand += left_operand.substring(start, end);
                    left = Long.parseLong(operand);
                    if (divided_tmp.length() > 0) {
                        step_diff = operand.length() - String.valueOf(left).length();
                        if (step_diff > 0 && operand.length() >= right_size && left > 0) {
                            step_diff += right_size;
                        } else if (step_diff == 0) {
                            step_diff = Math.min(right_size - operand_size, diff);
                        }
                        if (left >= right && Long.parseLong(String.valueOf(left).substring(0, right_size)) >= right) {
                            step_diff--;
                        }
                        if (step_diff > 0) {
                            divided_tmp.append(zero_repeat(step_diff));
                        }
                    }
                    if (left < right) {
                        operand = String.valueOf(left);
                    } else {
                        operand = String.valueOf(left % right);
                        divided_tmp.append(left / right);
                    }
                    if (operand.equals('0')) {
                        operand = "";
                    }
                    operand_size = operand.length();
                } while (end + 1 <= left_size);
            } else {
                //两运算数长度都在最大计算长度之外
                int multiple;
                //临时运算数据
                Number right_tmp = new Number(right_operand), operand_tmp;
                Number[] sub_number = {null, right_tmp, null, null, null, null, null, null, null, null};
                //运算数拆分
                right = Long.parseLong(right_operand.substring(0, step));
                do {
                    start = end;
                    diff = right_size - operand.length();
                    end += Math.max(diff, 1);
                    if (end > left_size) {
                        diff -= end - left_size;
                        end = left_size;
                    }
                    operand += left_operand.substring(start, end);
                    if (diff == 0) {
                        left = Long.parseLong(operand.substring(0, maxSize));
                    } else {
                        left = Long.parseLong(operand.substring(0, Math.min(step, operand.length())));
                        if (divided_tmp.length() > 0 && diff > 1) {
                            divided_tmp.append(zero_repeat(diff - 1));
                        }
                    }
                    if (right_size > operand.length() || (right_size == operand.length() && right_operand.charAt(0) > operand.charAt(0))) {
                        operand = criterion(operand);
                        multiple = 0;
                    } else {
                        operand_tmp = new Number(operand);
                        //取出大概商
                        multiple = (int) (left / right);
                        //商不可以超出
                        if (multiple > 0) {
                            if (multiple > 9) {
                                multiple = 9;
                            }
                            if (sub_number[multiple] == null) {
                                sub_number[multiple] = bcmul(new Number(String.valueOf(multiple)), right_tmp);
                            }
                            if (bccomp(sub_number[multiple], operand_tmp) > 0) {
                                multiple--;
                                if (sub_number[multiple] == null) {
                                    sub_number[multiple] = bcmul(new Number(String.valueOf(multiple)), right_tmp);
                                }
                            }
                            operand = bcsub(operand_tmp, sub_number[multiple]).operand;
                        }
                    }
                    divided_tmp.append(multiple);
                } while (end + 1 <= left_size);
            }
            divided = criterion(divided_tmp.toString());
        }
        result.operand = operand;
        result.divided = divided;
        result.point = left_number.point;
        if (!operand.equals('0')) {
            result.symbol = symbol;
        }
        return result;
    }

    /**
     * 计算两个数字相幂
     *
     * @param left_number 左运算数
     * @param right_number 右运算数
     * @return Number
     */
    protected static Number bcpow(Number left_number, Number right_number) throws Exception {
        String left_operand = left_number.operand,
                right_operand = right_number.operand;
        int right_point = right_number.point;
        char right_symbol = right_number.symbol;
        if (right_operand.equals("0")) {
            return new Number('1');
        }
        if (left_operand.equals("0")) {
//            if (right_symbol.equals("-")) {
//                throw new Exception('0 没有负幂');
//            }
            return new Number('0');
        }
        right_number.symbol = '+';
        Number one_number = new Number('1'),
                pow_current, pow_number, new_pow,
                right_denominator = null;
        //小数需要化简
        if (right_point != -1) {
            Number[] numbers = fraction(right_number);
            right_number = numbers[0];
            right_denominator = numbers[1];
        }
        int index = 0, key, size = right_number.operand.length() * 3 + 1;
        Number[] array = new Number[size],
                pows = new Number[size];
        char symbol = left_number.symbol;
        if (symbol == '-' && bcmod(right_number, new Number('2')).operand.equals("0")) {
            left_number.symbol = '+';
        }
        //计算出最大幂
        array[index] = pow_current = left_number;
        pows[index++] = pow_number = one_number;
        while (true) {
            new_pow = bcadd(pow_number.copy(), pow_number);
            if (bccomp(new_pow, right_number) <= 0) {
                pow_current = bcmul(pow_current, pow_current);
                array[index] = pow_current;
                pows[index++] = new_pow;
                pow_number = new_pow;
            } else {
                break;
            }
        }
        //补充缺少幂
        while (true) {
            right_number = bcsub(right_number, pow_number);
            if (right_number.operand.equals("0")) {
                break;
            }
            for (key = (right_number.operand.length() - 1) * 3; key < index && bccomp(pows[key], right_number) <= 0; key++);
            pow_number = pows[--key];
            pow_current = bcmul(pow_current, array[key]);
        }
        //小数幂
        // a^(b/c) = a^b~c
        if (right_point != -1) {
            pow_current = bcroot(pow_current, right_denominator);
        }
        //负数需要进行反转
        if (right_symbol == '-') {
            return bcdiv(one_number, pow_current, maxScale);
        }
        return pow_current;
    }

    /**
     * 计算一个数的方根值
     *
     * @param left_number 左运算数
     * @param right_number 右运算数
     * @return array
     */
    protected static Number bcroot(Number left_number, Number right_number) throws Exception {
        String left_operand = left_number.operand,
                right_operand = right_number.operand;
        char right_symbol = right_number.symbol;
        int right_point = right_number.point;
        if (right_operand.equals("1") || left_operand.equals("1")) {
            return left_number;
        }
        if (left_operand.equals("0")) {
            return left_number;
        }
        if (right_operand.equals("1")) {
            throw new Exception("不能取任何数的0次方根");
        }
        right_number.symbol = '+';
        char symbol;
        if (left_number.symbol == '-') {
            if (!bcmod(right_number, new Number('2')).operand.equals("0")) {
                throw new Exception("双数幂开不出负数方根");
            }
            symbol = '-';
            left_number.symbol = '+';
        } else {
            symbol = '+';
        }
        Number one_number = new Number('1'),
                diff, newroot, init_number, root;
        //小数需要化简
        if (right_point != -1) {
            Number[] numbers = fraction(right_number);
            right_number = numbers[0];
            // 小数方根，计算公式
            // a~(b/c) = a^c~b
            left_number = bcpow(left_number, numbers[1]);
        }
        // 计算出初始根近似值
        diff = bcsub(right_number.copy(), one_number);
        //大概计算根是几位数整数
        int int_size = left_operand.length() - left_number.point;
        Number mod = bcmod(new Number(String.valueOf(int_size)), right_number);
        int length = Integer.parseInt(mod.divided);
        if (!mod.operand.equals('0')) {
            length++;
        }
        init_number = new Number(left_operand.substring(0, Math.min(length, left_operand.length())));

        newroot = bcdiv(bcadd(bcmul(init_number.copy(), diff), bcdiv(left_number.copy(), bcpow(init_number.copy(), diff.copy()), maxScale)), right_number.copy(), length);
        // 任意数开任意次方的公式：牛顿迭代法，循环直到接近
        // a~b = (c*(b-1)+a/c^(b-1))/b;
        do {
            root = newroot.copy();
            newroot = bcdiv(bcadd(bcmul(root.copy(), diff.copy()), bcdiv(left_number.copy(), bcpow(root.copy(), diff.copy()), maxScale)), right_number.copy(), maxScale);
        } while (!root.operand.equals(newroot.operand));
        if (right_symbol == '-') {
            root = bcdiv(one_number, root, maxScale);
        }
        root.symbol = symbol;
        return root;
    }

}

/**
 * 高精度计算数值处理器
 */
class Number {

    /**
     * 数值正负号
     */
    protected char symbol;
    /**
     * 计算数值字符串
     */
    protected String operand;
    /**
     * 每次计算数值最大计算长度
     */
    protected int size = 0;
    /**
     * 计算数值拆分集
     */
    protected long[] items;
    /**
     * 计算数值拆分集坐标
     */
    protected int items_index = -1;
    /**
     * 小数位数
     */
    protected int point;
    /**
     * 除法后的商
     */
    protected String divided;

    /**
     * 以字符串初始数值
     *
     * @param operand
     */
    protected Number(String operand) {
        if (operand.charAt(0) == '+' || operand.charAt(0) == '-') {
            symbol = operand.charAt(0);
            operand = operand.substring(1);
        } else {
            symbol = '+';
        }
        if (operand.equals("0")) {
            point = -1;
            symbol = '+';
        } else {
            operand = BcMath.criterion(operand);
            point = operand.indexOf('.');
            if (point != -1) {
                point = operand.length() - point - 1;
                operand = BcMath.criterion(operand.replace(".", ""));
            }
        }
        this.operand = operand;
    }

    /**
     * 以单个字符初始数值
     *
     * @param operand
     */
    protected Number(char operand) {
        symbol = '+';
        point = -1;
        this.operand = "" + operand;
    }

    /**
     * 以数值拆分集初始数值
     *
     * @param items
     * @param size
     * @param index
     */
    protected Number(long[] items, int size, int index) {
        this('0');
        this.items = items;
        this.size = size;
        items_index = index;
    }

    /**
     * 以数值初始数值，即复制
     *
     * @param number
     */
    protected Number(Number number) {
        symbol = number.symbol;
        operand = number.operand;
        size = number.size;
        if (number.items != null) {
            items = number.items.clone();
        }
        items_index = number.items_index;
        point = number.point;
    }

    /**
     * 复制数值
     *
     * @return
     */
    public Number copy() {
        return new Number(this);
    }

    /**
     * 以字符串创建数值
     *
     * @param operand
     * @return
     * @throws Exception
     */
    protected static Number create(String operand) throws Exception {
        Number number = new Number(operand);
        if (number.operand.indexOf('.') != -1) {
            throw new Exception("not number");
        }
        return number;
    }

    /**
     * 判断数值是否相等
     *
     * @param number
     * @return
     */
    protected boolean equals(Number number) {
        return number.symbol == symbol && number.operand.equals(operand) && number.point == point && number.symbol == symbol;
    }

    /**
     * 获取数值的小数位数
     *
     * @return
     */
    protected int point() {
        return point > 0 ? point : 0;
    }

}
