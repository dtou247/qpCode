package com.sy599.game.common.executor.task;

import com.sy599.game.GameServerConfig;
import com.sy599.game.base.BaseTable;
import com.sy599.game.character.Player;
import com.sy599.game.common.executor.TaskExecutor;
import com.sy599.game.db.bean.group.GroupTable;
import com.sy599.game.db.bean.group.GroupUser;
import com.sy599.game.db.bean.redBagRain.GroupRedBagRainConfig;
import com.sy599.game.db.bean.redBagRain.GroupRedBagResult;
import com.sy599.game.db.dao.GroupRedBagRainDao;
import com.sy599.game.db.dao.group.GroupDao;
import com.sy599.game.manager.TableManager;
import com.sy599.game.util.GroupRedBagRainUtil;
import com.sy599.game.util.LogUtil;
      import org.apache.commons.lang.time.DateFormatUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * creatBy butao
 * date: 2021/5/24 0024
 * desc:定时结算 未领取的 亲友圈红包雨配置
 */
public class PushGroupRedBagRainResultTask implements  Runnable{
    @Override
    public void run() {
        LocalDateTime time = LocalDateTime.now() ;
        DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00");
        String startDate = dtf2.format(time);

        List<String> removeKey = new ArrayList<>();
        if( GroupRedBagRainUtil.needAccountRedMapRainConfigMap.size()>0) {
           for ( GroupRedBagRainConfig config : GroupRedBagRainUtil.needAccountRedMapRainConfigMap.values()) {
               String pst = DateFormatUtils.format(config.getPushendtime(), "yyyy-MM-dd HH:mm:ss");
               LocalDateTime time2 = LocalDateTime.parse(pst, dtf2);
               //结算时间验证
               LogUtil.msgLog.info("RedBagRainConfigResultTask|Start====================config:"+config.getKeyid() );
               if ((time.isEqual(time2) || time2.isBefore(time)) && config.getState() == 2) {
                   LogUtil.msgLog.info("RedBagRainConfigResult Task|id=" + config.getKeyid() + "|" + pst);
                   long groupId = config.getGroupid();
                   long configId =config.getKeyid();
                   //结算所有未领取红包；
                   config.setState(3);//结算
                   try {
                       List<GroupRedBagResult> list =    GroupRedBagRainDao.getInstance().loadAllUnTakeRedBag(configId);
                       if(null!=list && list.size()>0){
                           GroupUser  master =GroupDao.getInstance().loadGroupMaster(groupId+"");

                           int HuiZhangRedBagBackNum=0;
                           for (GroupRedBagResult item:list ) {
                               if(item.getUserid()>0 && item.getUserTakeState()==0){
                                    //日志 信用分
                                   GroupRedBagRainUtil.addCreditLog(groupId,item.getUserid(),item.getRedbagnum());
                                   GroupDao.getInstance().updateGroupCredit(String.valueOf(groupId),item.getUserid(), item.getRedbagnum());
                                   LogUtil.msgLog.info("RedBagRainConfigResult Task|PlayerAccount=" + config.getKeyid() + "|" + item.getRedbagnum());
                               }else if(item.getUserid()==0 && item.getUserTakeState()==0){
                                   //结算给群主
                                   HuiZhangRedBagBackNum+=item.getRedbagnum();
                              }

                           }
                           //会长日志
                           GroupRedBagRainUtil.addCreditLog(groupId,master.getUserId(),HuiZhangRedBagBackNum);
                           GroupDao.getInstance().updateGroupCredit(String.valueOf(groupId),master.getUserId(),HuiZhangRedBagBackNum);
                           LogUtil.msgLog.info("RedBagRainConfigResult Task|HuiZhang Account=" + config.getKeyid() + "|backNum=" + HuiZhangRedBagBackNum);

                           //更新 t_group_redBagRainResult
                           HashMap<String, Object> upResultState = new HashMap<>();
                           upResultState.put("keyId",config.getKeyid());
                           int r = GroupRedBagRainDao.getInstance().updateResultState(upResultState);

                           //更新t_group_redbagrain_config表
                           HashMap<String, Object> upstate = new HashMap<>();
                           upstate.put("state",3);//0未开始1发放中2已结束3已结算',
                           upstate.put("keyId",config.getKeyid());
                           int r1 = GroupRedBagRainDao.getInstance().updateConfigState(upstate);
                           removeKey.add(""+config.getKeyid());

                           System.err.println("结算 t_group_redBagRainResult："+r +" 条 redbagconfigId="+config.getKeyid());
                           System.err.println("结算 t_group_redbagrain_config："+r1 +" 条 keyid="+config.getKeyid());

                       }
                   }catch (Exception e){
                        LogUtil.errorLog.info("PushGroupRedBagRainResultTask|e:",e);
                   }
               }
           }
           if(removeKey.size()>0){
               for (String str:removeKey) {
                   GroupRedBagRainUtil.needAccountRedMapRainConfigMap.remove(str);
               }

           }
       }
    }

   public static int getNextMinDelay(){
     int second =  LocalTime.now().getSecond();
     return 60-second;
   }
}
