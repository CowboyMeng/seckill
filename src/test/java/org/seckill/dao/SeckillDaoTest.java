package org.seckill.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.entity.Seckill;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @Author: Chou_meng
 * @Date: 2017/11/10
 */

/**
 * 配置spring和junit的整合，junit启动时加载springIOC容器
 * spring-test junit
 */
@RunWith(SpringJUnit4ClassRunner.class)
// 告诉junit spring配置文件
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SeckillDaoTest {

    // 注入dao实现类依赖
    @Resource
    private SeckillDao seckillDao;

    @Test
    public void reduceNumber() throws Exception {
        // java没有保存形参的记录：reduceNumbeu(long seckillId, Date killTime) -> reduceNumber(arg0, arg1)
        // 所以，当dao接口的方法有多个输入参数时，必须对每个参数设定一个别名，否则java找不到参数对应的值。
        int updateCount = seckillDao.reduceNumber(1000, new Date());
        System.out.println(updateCount);
    }

    @Test
    public void queryById() throws Exception {
        Seckill seckill = seckillDao.queryById(1000L);
        System.out.println(seckill);
    }

    @Test
    public void queryAll() throws Exception {
        List<Seckill> seckills = seckillDao.queryAll(0, 100);
        for (Seckill seckill : seckills) {
            System.out.println(seckill);
        }
    }

}