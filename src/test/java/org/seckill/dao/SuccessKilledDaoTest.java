package org.seckill.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.entity.SuccessKilled;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * @Author: Chou_meng
 * @Date: 2017/11/10
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring/spring-dao.xml")
public class SuccessKilledDaoTest {

    @Resource
    private SuccessKilledDao successKilledDao;

    @Test
    public void insertSuccessKilled() throws Exception {
        long seckillId = 1001L;
        long userphone = 18801082263L;
        int insertCount = successKilledDao.insertSuccessKilled(seckillId, userphone);
        System.out.println("insertCount = " + insertCount);
    }

    @Test
    public void queryByIdWithSeckill() throws Exception {
        long seckillId = 1001L;
        long userphone = 18801082263L;
        SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userphone);
        System.out.println(successKilled);
    }

}