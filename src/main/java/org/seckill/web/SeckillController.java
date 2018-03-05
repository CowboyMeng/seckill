package org.seckill.web;

import org.seckill.Enum.SeckillStateEnum;
import org.seckill.Exception.RepeatSeckillException;
import org.seckill.Exception.SeckillCloseException;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.dto.SeckillResult;
import org.seckill.entity.Seckill;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * @Author: Chou_meng
 * @Date: 2017/11/15
 */
@Controller
@RequestMapping(value="/seckill")
public class SeckillController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @RequestMapping(value="/list", method= RequestMethod.GET, produces = {"application/json;charset=UTF-8"})
    public String list(Model model) {
        // 获取列表页
        List<Seckill> list = seckillService.getSeckillList();
        model.addAttribute("list", list);
        // list.jsp + model = ModelAndView
        return "list"; // /WEB-INFO/jsp/list.jsp

    }

    @RequestMapping(value="/{seckillId}/detail", method=RequestMethod.GET)
    public String detail(@PathVariable("seckillId") Long seckillId, Model model) {
        if (seckillId == null) {
            return "redirect:/seckill/list";
        }
        Seckill seckill = seckillService.getById(seckillId);
        if (seckill == null) {
            return "forward:/seckill/list";
        }
        model.addAttribute("seckill", seckill);
        return "detail";

    }

    // ajax json
    @RequestMapping(value="/{seckillId}/exposer",
            method=RequestMethod.POST,
            produces={"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer> exposer(@PathVariable("seckillId") Long seckillId) {
        SeckillResult<Exposer> result;
        try {
            Exposer exposer = seckillService.exposeSeckillUrl(seckillId);
            result = new SeckillResult<Exposer>(true, exposer);
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
            result = new SeckillResult<Exposer>(false, e.getMessage());
        }
        return result;
    }

    @RequestMapping(value="/{seckillId}/{md5}/execution",
                    method = RequestMethod.POST,
                    produces={"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable Long seckillId,
                                                   @PathVariable String md5,
                                                   @CookieValue(value="killPhone", required = false) Long userPhone) {
        // springmvc valid
        if (userPhone == null) {
            return new SeckillResult<SeckillExecution>(false, "未注册");
        }
        try {
            // 优化：存储过程调用
            SeckillExecution execution = seckillService.executeSeckillProcedure(seckillId, userPhone, md5);
//            SeckillExecution execution = seckillService.executeSeckill(seckillId, userPhone, md5);
            return new SeckillResult<SeckillExecution>(true, execution);
        } catch (SeckillCloseException e) {
            SeckillExecution exection = new SeckillExecution(seckillId, SeckillStateEnum.END);
            return new SeckillResult<SeckillExecution>(true, exection);
        } catch (RepeatSeckillException e) {
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            return new SeckillResult<SeckillExecution>(true, execution);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            return new SeckillResult<SeckillExecution>(true, execution);
        }
    }

    @RequestMapping(value="/time/now", method=RequestMethod.GET)
    @ResponseBody
    public SeckillResult<Long> time() {
        Date now = new Date();
        return new SeckillResult<Long>(true, now.getTime());
    }

}
