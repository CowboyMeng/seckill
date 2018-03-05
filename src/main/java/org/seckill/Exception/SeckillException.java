package org.seckill.Exception;

/**
 * @Author: Chou_meng
 * @Date: 2017/11/13
 */
public class SeckillException extends RuntimeException {

    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }

}
