/* 
 * 高精度计算
 */
(function (global) {
    /**
     * @var int 最大直接计算长度
     */
    var maxSize = 15;

    /**
     * @var int 最大直接计算值
     */
    var max = 1000000000000000;

    /**
     * @var int 最大直接乘法长度
     */
    var maxMulSize = 7;

    /**
     * @var int 最大直接乘法计算值
     */
    var maxMul = 10000000;

    /**
     * @var int 超大乘法计算长度
     */
    var hugeMulSize = 25;

    /**
     * @var int 最大直小数精度
     */
    var maxScale = 100;

    /**
     * @var{Object}支持的运算
     */
    var methods = {
        '+': bcadd,
        '-': bcsub,
        '*': bcmul,
        '/': bcdiv,
        '%': bcmod,
        '^': bcpow,
        '?': bccomp,
        '~': bcroot
    };

    /**
     * @var{Object}运算答优先级
     */
    var priority = {
        '?': 0,
        '+': 1,
        '-': 1,
        '*': 2,
        '/': 2,
        '%': 2,
        '^': 3,
        '~': 3
    };

    /**
     * 解析公式并计算处理
     * 支持运算符： ＋、－、*、/、%、^、~
     * 支持小括号
     * @param {String} formula   公式字符串
     * @param int scale        小数精度位数
     * @return {String}           计算结果字符串
     * @throws
     */
    function counter(formula, scale) {
        var length = formula.length,
                array = [],
                queue = [],
                number = '',
                type = 'none',
                float = '',
                bit = '',
                next, size, listen, oper, curr, num, val2, ope, val1, result;
        scale = scale || maxScale;
        for (var index = 0; index <= length; index++) {
            if (index < length) {
                switch (bit = formula[index]) {
                    case '(':
                        if (number !== '') {
                            throw 'error syntax: ' + formula.substr(index);
                        }
                        queue.push(array);
                        array = [];
                        continue;
                    case ')':
                        if (queue.length === 0) {
                            throw 'error syntax: ' + formula.substr(index);
                        }
                        type = 'ope';
                        break;
                    case "\r":
                    case "\n":
                    case "\t":
                    case ' ':
                        if (number !== '') {
                            type = 'ope';
                        }
                        continue;
                    default:
                        if (type === 'none' || type === 'val') {
                            type = 'val';
                            switch (bit) {
                                case '+':
                                case '-':
                                    if (number !== '') {
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
                                    if (float === '0') {
                                        throw 'error number: ' + formula.substr(index);
                                    }
                                    number += bit;
                                    continue;
                                case '.':
                                    if (float === '.') {
                                        throw 'error number: ' + formula.substr(index);
                                    }
                                    float = '.';
                                    if (number === '') {
                                        bit += '0';
                                    }
                                    number += bit;
                                    continue;
                                case '0':
                                    if (number === '') {
                                        float = '0';
                                    }
                                    number += bit;
                                    continue;
                            }
                        }
                        next = index + 1;
                        switch (bit) {
                            case '=':
                            case '?':
                            case '-':
                            case '+':
                            case '*':
                            case '/':
                            case '%':
                            case '^':
                            case '~':
                                if (type === 'ope' || (type === 'val' && number !== '' && number !== '-' && number !== '+')) {
                                    if (number !== '') {
                                        array.push(number);
                                    }
                                    array.push(bit);
                                    number = '';
                                    float = '';
                                    type = 'val';
                                    continue;
                                }
                        }
                        throw 'error formula: ' + formula.substr(index);
                }
            }
            if (number !== '') {
                array.push(number);
            }
            size = array.length;
            listen = [];
            for (oper = 0; size >= oper; oper += 2) {
                curr = '';
                if (size > oper) {
                    listen.push(array[oper]);
                    if (size > oper + 1) {
                        curr = array[oper + 1];
                    }
                }
                while (0 < (num = listen.length)) {
                    if (num > 2 && (curr === '' || priority[listen[num - 2]] >= priority[curr])) {
                        val2 = listen.pop();
                        ope = listen.pop();
                        val1 = listen.pop();
                        result = accuracy(Compute(methods[ope], val1, val2), scale);
                        if (result === null) {
                            throw 'Calculation error: ' + ope;
                        }
                        listen.push(result);
                    } else {
                        if (curr) {
                            listen.push(curr);
                        }
                        break;
                    }
                }
            }
            if (bit === ')') {
                array = queue.pop() || [];
            }
            if (listen.length === 1) {
                number = listen[0];
            } else {
                throw 'error syntax: ' + bit;
            }
        }
        if (queue.length > 0) {
            throw 'error syntax: ( not end';
        }
        return accuracy(number, scale);
    }

    /**
     * 创建数据集
     * @param {String} symbol
     * @param {String} operand
     * @param {Number} point
     * @param {Number} size
     * @param {Array} items
     * @returns {Object}
     */
    function createNumber(symbol, operand, point, size, items) {
        return {
            symbol: symbol,
            operand: operand,
            size: size,
            items: items,
            point: point,
            clone: function () {
                return createNumber(this.symbol, this.operand, this.point, this.size, this.items.concat([]));
            }
        };
    }
    /**
     * 创建数据
     * @param {String} operand   运算数
     * @return {Object}
     * @throws
     */
    function newNumber(operand) {
        if (operand[0] === '+' || operand[0] === '-') {
            var symbol = operand[0];
            operand = operand.substr(1);
        } else {
            var symbol = '+';
        }
        if (operand === '0') {
            var point = -1;
            symbol = '+';
        } else {
            operand = criterion(operand);
            var point = operand.indexOf('.');
            if (point !== -1) {
                point = operand.length - point - 1;
                operand = criterion(operand.replace('.', ''));
                if (operand.indexOf('.') !== -1) {
                    throw 'error number';
                }
            }
        }
        return createNumber(symbol, operand, point, 0, []);
    }

    /**
     * 整理为标准数字,去掉前后不合理的零和小数标准化
     * @param {String} operand   运算数
     * @return {String}
     */
    function criterion(operand) {
        var length = operand.length;
        if (length <= 1) {
            return operand || '0';
        }
        for (var int = 0; int < length && operand[int] === '0'; int++)
            ;
        var point = operand.indexOf('.');
        if (-1 !== point) {
            for (var decimal = length - 1; decimal >= point && operand[decimal] === '0'; decimal--)
                ;
            if (decimal < length) {
                if (operand[decimal] !== '.') {
                    decimal++;
                }
                operand = operand.substr(0, decimal);
            }
            if (point === int) {
                int--;
            }
        }
        if (int > 0) {
            operand = operand.substr(int);
            if (operand === '') {
                return '0';
            }
        }
        return operand;
    }

    /**
     * 放大同等级小数
     * @param int size             最大直接运算长度
     * @param {Object} left_object     左运算数
     * @param {Object} right_object    右运算数
     */
    function amplify(size, left_object, right_object) {
        var left_point = left_object.point > 0 ? left_object.point : 0, right_point = right_object.point > 0 ? right_object.point : 0;
        if (left_point !== right_point) {
            var zero = zero_repeat(Math.abs(left_point - right_point));
            if (left_point > right_point) {
                if (right_object.operand !== '0') {
                    right_object.operand += zero;
                    right_object.point = left_point;
                }
            } else {
                if (left_object.operand !== '0') {
                    left_object.operand += zero;
                    left_object.point = right_point;
                }
            }
        }
        var operands = [left_object, right_object], operand, items, number, length;
        for (var key = 0; key < operands.length; key++) {
            operand = operands[key];
            if (operand.size !== size) {
                items = [];
                operand.size = size;
                number = operand.operand;
                for (length = number.length; length > 0; length -= size) {
                    items.push(parseInt(length > size ? number.substr(length - size, size) : number.substr(0, length)));
                }
                operand.items = items;
            }
        }
    }

    /**
     * 重建数据参数
     * @param Object operand
     * @return {Object}
     */
    function rebuild(operand) {
        var size = operand.size;
        if (size > 0) {
            var number = '', key, item, length = operand.items.length;
            for (key = 0; key < length; key++) {
                item = String(operand.items[key]);
                number = zero_repeat(size - item.length) + item + number;
            }
            if (number[0] === '0') {
                operand.size = 0;
            }
            operand.operand = criterion(number);
        }
        return operand;
    }
    /**
     * 复制多个零
     * @param {Number} num
     * @returns {String}
     */
    function zero_repeat(num) {
        if (num > 0) {
            var zero = '0';
            while (num >= zero.length * 2) {
                zero += zero;
            }
            if (num > zero.length) {
                zero += zero.substr(0, num - zero.length);
            }
            return zero;
        }
        return '';
    }
    /**
     * 复原整数到指定精度小数
     * @param {Object} operand    拆分的数字
     * @param int scale        小数位数
     * @return {String}
     */
    function restore(operand, scale) {
        var number = operand.operand;
        scale = (operand.point > 0 ? operand.point : 0) + (scale || 0);
        if (scale > 0 && number !== '0') {
            var point = number.indexOf('.'),
                    length = number.length;
            if (point !== -1) {
                scale += length - point;
                number = number.replace('.', '');
                length = number.length;
            }
            if (length > scale) {
                var pos = length - scale,
                        int = number.substr(0, pos),
                        decimal = number.substr(pos);
            } else {
                var int = '0';
                if (length > 0) {
                    scale -= length;
                    var decimal = scale > 0 ? zero_repeat(scale) + number : number;
                } else {
                    var decimal = '';
                }
            }
            number = int;
            if (decimal !== '') {
                number += '.' + decimal;
            }
        }
        return criterion(number);
    }

    /**
     * 提取指定精度
     * @param {String} number
     * @param int scale
     * @return {String}
     */
    function accuracy(number, scale) {
        var point = number.indexOf('.');
        if (-1 !== point) {
            number = number.substr(0, point + scale + 1);
        }
        return criterion(number);
    }

    /**
     * 转换为最简分数
     * @param {Object} operand
     * @return {Object}
     */
    function fraction(operand) {
        var point = operand.point;
        if (point === -1) {
            return [operand, newNumber('1')];
        }
        operand.point = -1;
        var divisor = operand,
                dividend = newNumber('1' + zero_repeat(point)),
                denominator = dividend,
                remainder;
        while (dividend.operand !== '0') {
            remainder = bcmod(divisor, dividend);
            divisor = dividend;
            dividend = remainder;
        }
        if (divisor.operand === '1') {
            return [operand, denominator];
        }
        return [bcdiv(operand, divisor), bcdiv(denominator, divisor)];
    }
    /**
     * 跳相加
     * @param {Object} left_object  左运算数
     * @param {Object} right_object 右运算数
     * @return {Object}
     */
    function skipAdd(left_object, right_object, start) {
        if (left_object.symbol !== right_object.symbol) {
            left_object.operand += zero_repeat(start);
            left_object.size = 0;
            return bcadd(left_object, right_object);
        } else {
            var right_operand = right_object.operand,
                    right_size = right_operand.length;
            if (start >= right_size) {
                left_object.operand += zero_repeat(start - right_size) + right_operand;
            } else {
                var num = right_size - start,
                        result = bcadd(left_object, newNumber(right_operand.substr(0, num)));
                left_object.operand = result.operand + right_operand.substr(num);
            }
            left_object.size = 0;
        }
        return left_object;
    }

    /**
     * 比较两个数字大小
     * @param {Object} left_object  左运算数
     * @param {Object} right_object 右运算数
     * @return {Number}
     */
    function bccomp(left_object, right_object) {
        if (left_object.operand === right_object.operand && left_object.point === right_object.point && left_object.symbol === right_object.symbol) {
            return 0;
        }
        if (left_object.symbol !== right_object.symbol) {
            return left_object.symbol === '-' ? -1 : 1;
        } else if (left_object.symbol === '-') {
            left_object = left_object.clone();
            right_object = right_object.clone();
            left_object.symbol = '+';
            right_object.symbol = '+';
            return bccomp(right_object, left_object);
        }
        var left_int = left_object.operand.length - (left_object.point > 0 ? left_object.point : 0),
                right_int = right_object.operand.length - (right_object.point > 0 ? right_object.point : 0);
        if (left_int > right_int) {
            return 1;
        } else if (left_int > right_int) {
            return -1;
        }
        amplify(maxSize, left_object, right_object);
        var left_int_size = left_object.items.length,
                right_int_size = right_object.items.length;
        if (left_int_size > right_int_size) {
            return 1;
        } else if (left_int_size < right_int_size) {
            return -1;
        }
        var left = left_object.items,
                right = right_object.items;
        for (var key = left_int_size - 1; key >= 0; key--) {
            if (left[key] > right[key]) {
                return 1;
            } else if (left[key] < right[key]) {
                return -1;
            }
        }
        return 0;
    }

    /**
     * 计算两个数字相加
     * @param {Object} left_object  左运算数
     * @param {Object} right_object 右运算数
     * @return {Object}
     */
    function bcadd(left_object, right_object) {
        if (left_object.operand === '0') {
            return right_object;
        }
        if (right_object.operand === '0') {
            return left_object;
        }
        if (left_object.symbol !== right_object.symbol) {
            if (left_object.symbol === '-') {
                left_object.symbol = '+';
                return  bcsub(right_object, left_object);
            } else {
                right_object.symbol = '+';
                return  bcsub(left_object, right_object);
            }
        }
        amplify(maxSize, left_object, right_object);
        var left_items = left_object.items,
                right_items = right_object.items,
                next;
        if (left_items.length < right_items.length) {
            var tmp = left_object;
            left_object = right_object;
            right_object = tmp;
            left_items = left_object.items;
            right_items = right_object.items;
        }
        var length = right_items.length, key = 0;
        for (; key < length; key++) {
            left_items[key] += right_items[key];
            if (left_items[key] >= max) {
                next = key + 1;
                if (left_items[next] === undefined) {
                    left_items[next] = 1;
                } else {
                    left_items[next] += 1;
                }
                left_items[key] -= max;
            }
        }
        //向上进位
        if (right_items.length && left_items[key] !== undefined) {
            while (left_items[key] >= max) {
                next = key + 1;
                if (left_items[next] === undefined) {
                    left_items[next] = 1;
                } else {
                    left_items[next]++;
                }
                left_items[key++] -= max;
            }
        }
        left_object.items = left_items;
        return rebuild(left_object);
    }

    /**
     * 计算两个数字相减
     * @param {Object} left_object  左运算数
     * @param {Object} right_object 右运算数
     * @return {Object}
     */
    function bcsub(left_object, right_object) {
        if (left_object.operand === '0') {
            if (right_object.operand !== '0') {
                right_object.symbol = right_object.symbol === '-' ? '+' : '-';
            }
            return right_object;
        }
        if (right_object.operand === '0') {
            return left_object;
        }
        if (left_object.symbol !== right_object.symbol) {
            right_object.symbol = left_object.symbol;
            return bcadd(left_object, right_object);
        }
        amplify(maxSize, left_object, right_object);
        var comp = bccomp(left_object, right_object);
        if (comp === 0) {
            return newNumber('0');
        }
        if ((comp === 1 && left_object.symbol === '-') || (comp === -1 && left_object.symbol !== '-')) {
            right_object.symbol = left_object.symbol === '-' ? '+' : '-';
            var tmp = left_object;
            left_object = right_object;
            right_object = tmp;
        }
        var left_items = left_object.items,
                length = right_object.items.length,
                next, value, key = 0;
        for (; key < length; key++) {
            value = right_object.items[key];
            if (value > left_items[key]) {
                next = key + 1;
                left_items[next] -= 1;
                left_items[key] += max;
            }
            left_items[key] -= value;
        }
        //向上借位
        if (left_items[key] !== undefined) {
            while (left_items[key] < 0) {
                left_items[key++] += max;
                left_items[key]--;
            }
        }
        left_object.items = left_items;
        return rebuild(left_object);
    }

    /**
     * 计算两个数字相乘
     * @param {Object} left_object  左运算数
     * @param {Object} right_object 右运算数
     * @return {Object}
     */
    function bcmul(left_object, right_object) {
        var left_operand = left_object.operand,
                right_operand = right_object.operand,
                result = newNumber('0');
        if (left_operand === '0' || right_operand === '0') {
            return result;
        }
        var left_size = left_operand.length,
                right_size = right_operand.length,
                point = (left_object.point > 0 ? left_object.point : 0) + (right_object.point > 0 ? right_object.point : 0);
        if (left_size < hugeMulSize || right_size < hugeMulSize) {
            //禁止放大
            left_object.point = right_object.point = -1;
            amplify(maxMulSize, left_object, right_object);
            //直接计算
            var value = [],
                    left_length = left_object.items.length,
                    right_length = right_object.items.length,
                    right_items = right_object.items,
                    left, right, key, val, keyl = 0, keyr, mod, carry, next;
            result.size = maxMulSize;
            for (; keyl < left_length; keyl++) {
                left = left_object.items[keyl];
                for (keyr = 0; keyr < right_length; keyr++) {
                    right = right_items[keyr];
                    key = keyl + keyr;
                    val = left * right;
                    if (value[key] !== undefined) {
                        value[key] += val;
                    } else {
                        value[key] = val;
                    }
                    //处理进位
                    for (; value[key] >= maxMul; key++) {
                        //注意这里必需先取模，再减，否则运算时会导致精度丢失
                        mod = value[key] % maxMul;
                        carry = parseInt((value[key] - mod) / maxMul);
                        next = key + 1;
                        if (value[next] !== undefined) {
                            value[next] += carry;
                        } else {
                            value[next] = carry;
                        }
                        value[key] = mod;
                    }
                }
            }
            result.items = value;
        } else {
            /*
             * 分治乘法计算
             *  Let u = u0 + u1*(b^n)
             *  Let v = v0 + v1*(b^n)
             *  Then uv = (B^2n+B^n)*u1*v1 + B^n*(u1-u0)*(v0-v1) + (B^n+1)*u0*v0
             */
            var m = parseInt((Math.max(left_size, right_size) + 1) / 2);
            if (left_size <= m) {
                var x1 = '0',
                        x0 = left_operand;
            } else {
                var diff = left_size - m,
                        x1 = left_operand.substr(0, diff),
                        x0 = left_operand.substr(diff);
            }
            if (right_size <= m) {
                var y1 = '0',
                        y0 = right_operand;
            } else {
                var diff = right_size - m,
                        y1 = right_operand.substr(0, diff),
                        y0 = right_operand.substr(diff);
            }
            x1 = newNumber(x1);
            y1 = newNumber(y1);
            if (x1.operand !== '0' && y1.operand !== '0') {
                var z2 = bcmul(x1, y1);
                result = skipAdd(z2.clone(), result, m * 2);
                result = skipAdd(z2, result, m);
            }
            x0 = newNumber(x0);
            y0 = newNumber(y0);
            var z1 = bcmul(bcsub(x1, x0.clone()), bcsub(y0.clone(), y1));
            result = skipAdd(z1, result, m);
            if (x0.operand !== '0' && y0.operand !== '0') {
                var z0 = bcmul(x0, y0);
                result = bcadd(z0.clone(), result);
                result = skipAdd(z0, result, m);
            }
        }
        result.point = point;
        if (result.point === 0) {
            result.point = -1;
        }
        if (left_object.symbol !== right_object.symbol) {
            result.symbol = '-';
        }
        return rebuild(result);
    }

    /**
     * 计算两个数字相除
     * @param {Object} left_object  左运算数
     * @param {Object} right_object 右运算数
     * @param int scale 小数精度位数
     * @return array
     */
    function bcdiv(left_object, right_object, scale) {
        amplify(maxSize, left_object, right_object);
        var mod = bcmod(left_object, right_object);
        // 提取商
        var value = mod.divided;
        if (left_object.symbol !== right_object.symbol) {
            value = '-' + value;
        }
        // 处理余数
        if (mod.operand !== '0') {
            if (scale === undefined) {
                scale = maxScale;
            }
            mod.point = right_object['point'] = false;
            var right_size = right_object.operand.length, mod_size = mod.operand.length;
            mod.operand += zero_repeat(scale);
            var result = bcmod(mod, right_object), decimal = result.divided;
            if (decimal !== '0') {
                //计算最大填充零数
                var zero_size = right_size - mod_size;
                if (zero_size > 0) {
                    right_object.symbol = '+';
                    if (bccomp(newNumber(mod.operand.substr(0, right_size)), right_object) >= 0) {
                        zero_size--;
                    }
                    if (zero_size > 0) {
                        decimal = zero_repeat(zero_size) + decimal;
                    }
                }
                value += '.' + decimal;
            }
        }
        return newNumber(value);
    }

    /**
     * 除法计算
     * @param array left_object
     * @param array right_object
     * @return array
     */
    function bcmod(left_object, right_object) {
        var symbol = left_object.symbol,
                result = newNumber('0');
        amplify(maxSize, left_object, right_object);
        var left_operand = left_object.operand,
                right_operand = right_object.operand;
        //某运算数为0不需要计算
        if (left_operand === '0' || right_operand === '0') {
            result.divided = '0';
            return result;
        }
        //两个运算数相等，直接为 1
        if (left_operand === right_operand) {
            result.divided = '1';
            result.symbol = symbol;
            return result;
        }
        var left_size = left_operand.length,
                right_size = right_operand.length;
        //左运算数小于右运算数，无需计算
        if (left_size < right_size) {
            left_object.divided = '0';
            return left_object;
        }
        //两运算数长度都在最大计算长度之内
        if (left_size <= maxSize) {
            var operand = String(left_operand % right_operand),
                    divided = String(parseInt((left_operand - operand) / right_operand));
        } else {
            var operand = "",
                    divided = "",
                    end = 0,
                    start,
                    diff,
                    left,
                    step = maxSize - 1;
            //右运算数长度在最大计算长度之内
            if (right_size < maxSize) {
                right_operand = parseInt(right_operand);
                var operand_size = 0, step_diff;
                do {
                    start = end;
                    diff = step - operand_size;
                    end += Math.max(diff, 1);
                    if (end > left_size) {
                        diff -= end - left_size;
                        end = left_size;
                    }
                    operand += left_operand.substr(start, end - start);
                    left = parseInt(operand);
                    if (divided !== '') {
                        step_diff = operand.length - String(left).length;
                        if (step_diff > 0 && operand.length >= right_size && left > 0) {
                            step_diff += right_size;
                        } else if (step_diff == 0) {
                            step_diff = Math.min(right_size - operand_size, diff);
                        }
                        if (left >= right_operand && String(left).substr(0, right_size) >= right_operand) {
                            step_diff--;
                        }
                        if (step_diff > 0) {
                            divided += zero_repeat(step_diff);
                        }
                    }
                    if (left < right_operand) {
                        operand = String(left);
                    } else {
                        operand = String(left % right_operand);
                        divided += String((left - operand) / right_operand);
                    }
                    if (operand === '0') {
                        operand = '';
                    }
                    operand_size = operand.length;
                } while (end + 1 <= left_size);
            } else {
                //两运算数长度都在最大计算长度之外
                //临时运算数据
                var right_tmp = newNumber(right_operand),
                        sub_number = [0, right_tmp],
                        multiple,
                        operand_tmp,
                        //运算数拆分
                        right = parseInt(right_operand.substr(0, step));
                do {
                    start = end;
                    diff = right_size - operand.length;
                    end += Math.max(diff, 1);
                    if (end > left_size) {
                        diff -= end - left_size;
                        end = left_size;
                    }
                    operand += left_operand.substr(start, end - start);
                    if (diff > 1 && divided !== '') {
                        divided += zero_repeat(diff - 1);
                    }
                    if (right_size > operand.length || (right_size === operand.length && right_operand[0] > operand[0])) {
                        operand = criterion(operand);
                        multiple = 0;
                    } else {
                        left = parseInt(operand.substr(0, diff === 0 ? maxSize : step));
                        //取出大概商
                        multiple = parseInt(left / right);
                        if (multiple > 0) {
                            if (multiple > 9) {
                                multiple = 9;
                            }
                            operand_tmp = newNumber(operand);
                            if (sub_number[multiple] === undefined) {
                                sub_number[multiple] = bcmul(newNumber(String(multiple)), right_tmp);
                            }
                            //商不可以超出
                            if (bccomp(sub_number[multiple], operand_tmp) > 0) {
                                multiple--;
                                if (sub_number[multiple] === undefined) {
                                    sub_number[multiple] = bcmul(newNumber(String(multiple)), right_tmp);
                                }
                            }
                            operand = bcsub(operand_tmp, sub_number[multiple]).operand;
                        }
                    }
                    divided += String(multiple);
                } while (end + 1 <= left_size);
                divided = criterion(divided);
            }
        }
        result.operand = operand;
        result.divided = divided;
        result.point = left_object.point;
        if (operand != '0') {
            result.symbol = symbol;
        }
        return result;
    }


    /**
     * 计算两个数字相幂
     * @param {Object} left_object  左运算数
     * @param {Object} right_object 右运算数
     * @return {Object}
     */
    function bcpow(left_object, right_object) {
        var left_operand = left_object.operand,
                right_operand = right_object.operand,
                right_point = right_object.point,
                right_symbol = right_object.symbol;
        if (right_operand === '0') {
            return newNumber('1');
        }
        if (left_operand === '0') {
            return newNumber('0');
        }
        right_object.symbol = '+';
        //小数需要化简
        if (right_point !== -1) {
            var _fraction = fraction(right_object.clone()),
                    right_denominator = _fraction[1];
            right_object = _fraction[0];
        }
        var symbol = left_object.symbol,
                //计算出最大幂
                one_array = newNumber('1'),
                array = [],
                pows = [],
                pow_current = left_object,
                pow_array = one_array,
                new_pow, key, pow_length;
        if (symbol === '-' && bcmod(right_object, newNumber('2')).operand === '0') {
            left_object.symbol = '+';
        }
        array.push(pow_current);
        pows.push(pow_array);
        while (true) {
            new_pow = bcadd(pow_array.clone(), pow_array);
            if (bccomp(new_pow, right_object) <= 0) {
                pow_current = bcmul(pow_current, pow_current);
                pow_array = new_pow;
                array.push(pow_current);
                pows.push(pow_array);
            } else {
                break;
            }
        }
        pow_length = pows.length;
        //补充缺少幂
        while (true) {
            right_object = bcsub(right_object, pow_array);
            if (right_object.operand === '0') {
                break;
            }
            for (key = (right_object.operand.length - 1) * 3; key < pow_length && bccomp(pows[key], right_object) <= 0; key++)
                ;
            pow_array = pows[--key];
            pow_current = bcmul(pow_current, array[key]);
        }
        //小数幂
        // a^(b/c) = a^b~c
        if (right_point !== -1) {
            pow_current = bcroot(pow_current, right_denominator);
        }
        //负数需要进行反转
        if (right_symbol === '-') {
            return bcdiv(one_array, pow_current);
        }
        return pow_current;
    }

    /**
     * 计算一个数的方根值
     * @param {Object} left_object  左运算数
     * @param {Object} right_object 右运算数
     * @return {Object}
     */
    function bcroot(left_object, right_object) {
        var left_operand = left_object.operand,
                right_operand = right_object.operand,
                right_symbol = right_object.symbol,
                right_point = right_object.point;
        if (right_operand === '1' || left_operand === '1') {
            return left_object;
        }
        if (left_operand === '0') {
            return left_object;
        }
        if (right_operand === '0') {
            throw '不能取任何数的0次方根';
        }
        right_object.symbol = '+';
        if (left_object.symbol === '-') {
            var mod = bcmod(right_object, newNumber('2')).operand,
                    symbol = '-';
            if (mod === '0') {
                throw '双数幂开不出负数方根';
            }
            left_object.symbol = '+';
        } else {
            var symbol = '+';
        }
        //小数需要化简
        if (right_point !== false) {
            var _fraction = fraction(right_object.clone()),
                    right_denominator = _fraction[1];
            right_object = _fraction[0];
            // 小数方根，计算公式
            // a~(b/c) = a^c~b
            left_object = bcpow(left_object, right_denominator);
        }
        var one_array = newNumber('1'),
                // 计算出初始根近似值
                diff = bcsub(right_object.clone(), one_array),
                //大概计算根是几位数整数
                int_size = left_operand.length - left_object.point,
                zoom_array = newNumber(String(Math.abs(int_size))),
                root;
        if (bccomp(bcmul(zoom_array, newNumber('2')), right_object) < 0) {
            if (int_size > 0) {
                var newroot = newNumber('1.9');
            } else {
                var newroot = newNumber('0.9');
            }
        } else {
            zoom_array = newNumber(bcmod(zoom_array, right_object).divided);
            if (bccomp(zoom_array, newNumber('0')) === 0) {
                zoom_array = one_array;
            }
            //预估初始根值，越接近循环次数越少
            // 任意数开任意次方的公式：牛顿迭代法，循环直到接近
            // a~b = (c*(b-1)+a/c^(b-1))/b;
            //提取前两位数第一次收敛
            var min_array = newNumber(left_operand.substr(0, 2)),
                    zoom = bcpow(newNumber('10'), zoom_array);
            //计算初始根值
            newroot = bcdiv(bcadd(bcmul(one_array, diff), bcdiv(min_array, bcpow(one_array, diff.clone()))), right_object.clone());
            if (int_size > 0) {
                //有整数则缩放倍数
                newroot = bcmul(newroot, zoom);
            } else {
                //只有小数则放大倍数
                newroot = bcdiv(newroot, zoom);
            }
            //取标准收敛值
            var initroot = bcdiv(bcadd(bcmul(newroot.clone(), diff), bcdiv(left_object.clone(), bcpow(newroot.clone(), diff.clone()))), right_object.clone());
            //对比大小
            switch (bccomp(newroot.clone(), initroot)) {
                case 1:
                    newroot = bcdiv(newroot, newNumber('2'));
                    break;
                case - 1:
                    newroot = bcmul(newroot, newNumber('2'));
                    break;
            }
        }
        // 任意数开任意次方的公式：牛顿迭代法，循环直到接近
        // a~b = (c*(b-1)+a/c^(b-1))/b;
        var n = 0;
        do {
            root = newroot.clone();
            newroot = bcdiv(bcadd(bcmul(root.clone(), diff.clone()), bcdiv(left_object.clone(), bcpow(root, diff.clone()))), right_object.clone());
        } while (root.operand !== newroot.operand);
        if (right_symbol === '-') {
            newroot = bcdiv(one_array, newroot);
        }
        newroot.symbol = symbol;
        return newroot;
    }

    /**
     * 调用运算处理
     * @param {String} method        运算方法
     * @param {String} left_operand  左运算数
     * @param {String} right_operand 右运算数
     * @return {String}
     */
    function Compute(method, left_operand, right_operand) {
        //数据校验
        var result = method(newNumber(left_operand), newNumber(right_operand));
        if (typeof result === 'object') {
            var symbol = result.symbol === '-' ? '-' : '';
            return symbol + restore(result);
        }
        return String(result);
    }
    global.bcmath = counter;
})(window);
