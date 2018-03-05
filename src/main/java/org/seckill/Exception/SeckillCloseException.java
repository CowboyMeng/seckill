package org.seckill.Exception;

/**
 * @Author: Chou_meng
 * @Date: 2017/11/13
 */

/**
 * 秒杀关闭异常
 */
public class SeckillCloseException extends SeckillException {

    public SeckillCloseException(String message) {
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
