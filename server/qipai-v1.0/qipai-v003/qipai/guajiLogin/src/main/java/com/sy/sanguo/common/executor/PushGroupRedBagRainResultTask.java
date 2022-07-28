package com.sy.sanguo.common.executor;

import com.sy.sanguo.game.bean.group.GroupRedBagRainConfig;
import com.sy.sanguo.game.bean.group.GroupRedBagResult;
import com.sy.sanguo.game.bean.group.GroupUser;
import com.sy.sanguo.game.dao.ActivityDao;
import com.sy.sanguo.game.utils.GroupUtil;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * creatBy butao
 * date: 2021/5/24 0024
 * desc:定时结算 未领取的 玩法红包；结算无人抢可返回群主的红包
 */
public class PushGroupRedBagRainResultTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("sys");

    @Override
    public void run() {
        try {
            LocalDateTime time = LocalDateTime.now();
            DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00");
            DateTimeFormatter dtf3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String startDate = dtf2.format(time);//
            String endDate = dtf3.format(time.plusMinutes(-2));//

            HashMap<String, Object> map = new HashMap<>();
            map.put("startDate", startDate);
            map.put("state", 2);
            map.put("endDate", endDate);
            // select * from t_redbagrainconfig where state =2 and pushstarttime>startDate
            //System.err.println("Start1====================================" + map.toString());
            LOGGER.info("RedBagRain|PushGroupRedBagRainResultTask|start");
            List<GroupRedBagRainConfig> configList = ActivityDao.getInstance().loadNeedMasterAccountRedBagRainConfig(map);
            if (null != configList && configList.size() > 0) {
                // 群主 领取未抢得红包
                for (GroupRedBagRainConfig config : configList) {
                    //System.err.println("Start2====================================" + map.toString());
                    String pst = DateFormatUtils.format(config.getPushstarttime(), "yyyy-MM-dd HH:mm:ss");
                    LocalDateTime time2 = LocalDateTime.parse(pst, dtf3);
                    time2.plusSeconds(20);//红包发放10S 延迟20S后判定
                    //结算时间验证 清理发完的红包没人领取的返回群主
                    //System.err.println("时间验证···" + time.isAfter(time2) + " config.state=" + config.getState());
                    if (time.isAfter(time2) && config.getState() == 2) {
                        //System.err.println("时间验证通过···|id=" + config.getKeyid() + "|" + pst);
                        long groupId = config.getGroupid();
                        long configId = config.getKeyid();

                        // select * from t_group_redBagRainResult where userid=0 and usertakestate=0 and redbagconfigid=config.id;
                        List<GroupRedBagResult> list = ActivityDao.getInstance().loadAllUnTakeRedBag(configId);

                        HashMap<String, Object> map2 = new HashMap<>();
                        map2.put("keyId", config.getKeyid());
                        map2.put("state", 3);
                        //更新t_group_redbagrain_config表  //更新状态为结算 。。。。
                        int updateResult = ActivityDao.getInstance().updateGroupRedBagRainConfig(map2);
                        //System.err.println(updateResult + " =updateResult 更新t_group_redbagrain_config:" + map2.toString());
                        if (updateResult == 0) {
                            //更新状态失败 已结算
                            continue;
                        }
                        GroupUser master = ActivityDao.getInstance().loadGroupMaster(groupId);
                        //更新 t_group_redBagRainResult 中群主可以返回得红包
                        ////  UPDATE t_group_redBagRainResult SET usertakeState = 2,userid=#qunzhu# WHERE   redbagconfigid=3071 and usertakeState=0 AND userid=0;
                        HashMap<String, Object> upResultState = new HashMap<>();
                        upResultState.put("keyId", config.getKeyid());
                        upResultState.put("userId", master.getUserId());
                        upResultState.put("state", 2);//群主结算

                        int r = ActivityDao.getInstance().updateRedBagResult(upResultState);
                        //System.err.println(r + " =r t_group_redBagRainResult:" + map2.toString());

                        if (r <= 0) {
                            //已被更新
                            continue;
                        }
                        if (null != list && list.size() > 0) {
                            int HuiZhangRedBagBackNum = 0;
                            for (GroupRedBagResult item : list) {
                                if (item.getUserid() == 0 && item.getUserTakeState() == 0) {
                                    //结算给群主
                                    HuiZhangRedBagBackNum += item.getRedbagnum();
                                }
                            }
                            //会长日志
                            HashMap<String, Object> updateMap = new HashMap<>();
                            updateMap.put("keyId", master.getKeyId());
                            updateMap.put("credit", HuiZhangRedBagBackNum);
                            ActivityDao.getInstance().updateGroupUserCredit(updateMap);
                            ActivityDao.getInstance().insertGroupCreditLog(GroupUtil.redBagRainUpdateCreateLog(groupId, HuiZhangRedBagBackNum, master.getUserId()));
                            LOGGER.info("RedBagRain|"+config.getGroupid()+"|回退群主 "+master.getUserId()+"|configId="+config.getKeyid()+"|"+HuiZhangRedBagBackNum);
                            //System.err.println("结算 t_group_redBagRainResult：" + r + " 条 redbagconfigId=" + config.getKeyid() + " 返回数目：" + HuiZhangRedBagBackNum);
                            //System.err.println("结算 t_group_redbagrain_config：" + updateResult + " 条 keyid=" + config.getKeyid());
                        }

                    }

                }

            }

            //<!--    select * from t_group_redbagrain_config where state =3 and pushendtime>=#endDate#-->
            List<GroupRedBagRainConfig> configList2 = ActivityDao.getInstance().loadNeedMemberAccountRedBagRainConfig(map);

            if (null != configList2 && configList2.size() > 0) {
                for (GroupRedBagRainConfig config : configList2) {
                    String pst = DateFormatUtils.format(config.getPushendtime(), "yyyy-MM-dd HH:mm:ss");
                    LocalDateTime endtime = LocalDateTime.parse(pst, dtf3);
                    if (endtime.isBefore(LocalDateTime.now()) && config.getState() == 3) {
                        //结束时间《当前时间
                        //进行用户提取结算
                        List<GroupRedBagResult> redBagResults = ActivityDao.getInstance().loadAllUserUnTakeRedBag(config.getKeyid());
                        for (GroupRedBagResult item : redBagResults) {
                            if (item.getUserTakeState() == 1) {
                                continue;
                            }
                            //标记为已提取
                            HashMap<String, Object> takeMap = new HashMap<>();
                            takeMap.put("userId", item.getUserid());
                            takeMap.put("keyId", item.getRedBagConfigId());

                            int result = ActivityDao.getInstance().updateUserTakeRedBag(takeMap);
                            //System.err.println(result + " 更新用户红包提取记录：takeMap:" + takeMap.toString());
                            if (result == 1) {
                                //标记更新成功 变更信用分
                                GroupUser self = ActivityDao.getInstance().loadGroupUser(item.getGroupid(), item.getUserid());
                                //变更群内比赛分：	keyId =#keyId#
                                //			AND credit + #credit# <![CDATA[ >= ]]> 0
                                HashMap<String, Object> updateMap = new HashMap<>();
                                updateMap.put("keyId", self.getKeyId());
                                updateMap.put("credit", item.getRedbagnum());

                                int r2 = ActivityDao.getInstance().updateGroupUserCredit(updateMap);
                                //System.err.println(r2 + " 更新用户信用分 updateMap:" + updateMap.toString());

                                int credit = Integer.valueOf(item.getRedbagnum() + "");
                                ActivityDao.getInstance().insertGroupCreditLog(GroupUtil.redBagRainUpdateCreateLog(item.getGroupid(), credit, item.getUserid()));
                                LOGGER.info("RedBagRain|"+ item.getGroupid()+"|自动领取|"+self.getUserId()+"|configKeyId="+config.getKeyid()+"|"+item.getRedbagnum()+"|"+item.getId());

                                //System.err.println("自动到账：");
                                //System.err.println("自动到账数量：" + item.getRedbagnum());
                                //System.err.println("自动到账configkeyId：" + config.getKeyid());
                                //System.err.println("自动到账groupId：" + item.getGroupid());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getNextMinDelay() {
        int second = LocalTime.now().getSecond();
        return (60 - second+new Random().nextInt(6)) * 1000;
    }


//    class Task implements  Runnable{
//
//        @Override
//        public void run() {
//
//        }
//    }
}
