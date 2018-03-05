package org.seckill.service.Impl;

import org.apache.commons.collections.MapUtils;
import org.seckill.Enum.SeckillStateEnum;
import org.seckill.Exception.RepeatSeckillException;
import org.seckill.Exception.SeckillCloseException;
import org.seckill.Exception.SeckillException;
import org.seckill.dao.SeckillDao;
import org.seckill.dao.SuccessKilledDao;
import org.seckill.dao.cache.RedisDao;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.entity.SuccessKilled;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Chou_meng
 * @Date: 2017/11/14
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SuccessKilledDao successKilledDao;

    @Autowired
    private RedisDao redisDao;

    // md5盐值字符串，用于混淆MD5
    private String salt = "djfajfoiufefufhasfi";

    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0, 4);
    }

    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    // 暴露秒杀接口
    public Exposer exposeSeckillUrl(long seckillId) {

        // 优化点：缓存优化：超时的基础上维护一致性
        // 1 访问redis
        Seckill seckill = redisDao.getSeckill(seckillId);

        if (seckill == null) {
            // 2 访问数据库
            seckill = seckillDao.queryById(seckillId);
            if (seckill == null) {
                return new Exposer(false, seckillId);
            } else {
                // 3 放入redis
                redisDao.putSeckill(seckill);
            }
        }
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        // 系统当前时间
        Date nowTime = new Date();
        if (nowTime.getTime() < startTime.getTime() || nowTime.getTime() > endTime.getTime()) {
            return new Exposer(false, seckillId, nowTime.getTime(), startTime.getTime(), endTime.getTime());
        }
        // 转化特定字符串的过程，不可逆
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);
    }

    private String getMD5(long seckillId) {
        String base = seckillId + "/" + salt;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    /**
     * 使用注解控制事务的优点：
     * 1：开发团队达成一致约定，明确规定标注事务方法的编程风格
     * 2：保证事务方法的执行时间尽可能短，不要穿插其他网路操作RPC/HTTP请求或者剥离到事务方法外部
     * 3：不是所有的方法都需要事务，如只有一条修改操作，只读操作不需要事务控制
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatSeckillException
     * @throws SeckillCloseException
     */
    @Transactional
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatSeckillException, SeckillCloseException {
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            throw new SeckillException("seckill data rewrite");
        }

        try {
            // 执行秒杀。减库存 + 插入秒杀记录
            Date nowTime = new Date();

            // 深度优化：将先update再insert改为先insert再update
            // 插入秒杀记录
            // 记录购买行为 唯一：seckillId + userPhone
            int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            if (insertCount <= 0) {
                // 重复秒杀
                throw new RepeatSeckillException("seckill repeated");
            } else {
                // 减库存，热点商品竞争，行级锁加锁。
                int updateCount = seckillDao.reduceNumber(seckillId, nowTime);

                if (updateCount <= 0) {
                    // 没有更新到记录，秒杀结束 rollback
                    // 可能是秒杀结束了或者秒杀商品没有库存了
                    throw new SeckillCloseException("seckill is closed");
                } else {
                    // 秒杀成功 commit
                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
                }
            }

//            // 减库存，热点商品竞争，行级锁加锁。
//            int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
//
//            if (updateCount <= 0) {
//                // 没有更新到记录，秒杀结束 rollback
//                // 可能是秒杀结束了或者秒杀商品没有库存了
//                throw new SeckillCloseException("seckill is closed");
//            } else {
//                int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
//                if (insertCount <= 0) {
//                    // 重复秒杀
//                    throw new RepeatSeckillException("seckill repeated");
//                } else {
//                    // 秒杀成功 commit
//                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
//                    System.out.println("秒杀成功");
//                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
//                }
//            }

        } catch(SeckillCloseException e) {
            throw e;
        } catch(RepeatSeckillException e) {
            throw e;
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
            // 所有编译期异常，转化为运行期异常
            throw new SeckillException("seckill inner error: " + e.getMessage());
        }
    }

    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) {
        if (md5 == null || !md5.equals(this.getMD5(seckillId))) {
            return new SeckillExecution(seckillId, SeckillStateEnum.DATA_REWRITE);
        }
        Date killTime = new Date();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("seckillId", seckillId);
        map.put("phone", userPhone);
        map.put("killTime", killTime);
        map.put("result", null);
        // 执行存储过程，result被赋值
        try {
            seckillDao.killByprocedure(map);
            // 获取result
            int result = MapUtils.getInteger(map, "result", -2);
            if (result == 1) {
                SuccessKilled sk = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, sk);
            } else {
                return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(result));
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }
}
