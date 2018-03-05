package org.seckill.Exception;

/**
 * @Author: Chou_meng
 * @Date: 2017/11/13
 */

/**
 * 重复秒杀异常（运行期异常）
 */
public class RepeatSeckillException extends SeckillException {


    public RepeatSeckillException(String message) {
        super(message);
    }

    public RepeatSeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
