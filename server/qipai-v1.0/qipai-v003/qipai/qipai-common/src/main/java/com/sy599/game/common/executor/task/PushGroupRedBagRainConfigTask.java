package com.sy599.game.common.executor.task;

import com.sy599.game.GameServerConfig;
import com.sy599.game.base.BaseTable;
import com.sy599.game.character.Player;
import com.sy599.game.common.executor.TaskExecutor;
import com.sy599.game.db.bean.group.GroupInfo;
import com.sy599.game.db.bean.group.GroupTable;
import com.sy599.game.db.bean.redBagRain.GroupRedBagRainConfig;
import com.sy599.game.db.dao.RedBagInfoDao;
import com.sy599.game.db.dao.group.GroupDao;
import com.sy599.game.manager.TableManager;
import com.sy599.game.util.GroupRedBagRainUtil;
import com.sy599.game.util.LogUtil;
import com.sy599.game.util.ResourcesConfigsUtil;
import com.sy599.game.websocket.constant.WebSocketMsgType;
import org.apache.commons.lang.time.DateFormatUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

/**
 * creatBy butao
 * date: 2021/5/24 0024
 * desc:定时推送 亲友圈红包雨配置
 */
public class PushGroupRedBagRainConfigTask implements  Runnable{
    @Override
    public void run() {
        LocalDateTime time = LocalDateTime.now() ;
        DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00");
        String startDate = dtf2.format(time);
//        int control =   ResourcesConfigsUtil.loadIntegerValue("RedBagConfig","isOpen");
        LogUtil.msgLog.info("RedBagRain|PushGroupRedBagRainConfigTask");
//        if(control!=1){
//            //全部关闭
//            return;
//        }
       if( GroupRedBagRainUtil.needPushRedMapRainConfigMap.size()>0) {
           for ( GroupRedBagRainConfig config : GroupRedBagRainUtil.needPushRedMapRainConfigMap.values()) {
               String pst = DateFormatUtils.format(config.getPushstarttime(), "yyyy-MM-dd HH:mm:00");
               LocalDateTime time2 = LocalDateTime.parse(pst, dtf2);
               //是否可推送
               if ((time.isEqual(time2) || time2.isBefore(time)) && config.getState() == 0) {
                   LogUtil.msgLog.info("PushGroupRedBagRainConfigTask|id=" + config.getKeyid() + "|" + pst);
                   long groupId = config.getGroupid();
                   int serverId = GameServerConfig.SERVER_ID;

                   List<GroupTable> tableList = GroupDao.getInstance().loadGroupPlayingTables(groupId, serverId);
                   //推送前 变更红包雨配置状态 1发放中
                   config.setState(1);
                   GroupRedBagRainUtil.needPushRedMapRainConfigMap.put(config.getKeyid()+"",config);
                   GroupRedBagRainUtil.updateConfigState(config.getKeyid(),1);
                   LogUtil.msgLog.info("RedBagRain|PushGroupRedBagRainConfigTask|update State|id=" + config.getKeyid() + "|" + pst);

                   if (null != tableList && tableList.size() > 0) {
                       for (GroupTable table : tableList) {
                           BaseTable table1 = TableManager.getInstance().getTable(table.getTableId());
                           if(table1==null){
                               //System.err.println("table is null~~~~~~"+table.getTableId());
                              continue;
                           }
                           Map<Long, Player> playerMap = table1.getPlayerMap();
                           for (Player p : playerMap.values()) {
                               if(p.isRobot()){
                                   continue;
                               }
                               p.writeComMessage(WebSocketMsgType.resp_code_GroupRedBagRain_push, config.getKeyid() + "", groupId + "");
                           }
                           LogUtil.msgLog.info("RedBagRain|PushGroupRedBagRainConfigTask|pushTable:"+table.getTableId()+"|ServerId="+serverId);
                       }
                   }
                   //红包雨持续10S 10S后关闭抢的功能
                   TaskExecutor.TIMER.schedule(new TimerTask() {
                       @Override
                       public void run() {
                           config.setState(2);//结束
                           GroupRedBagRainUtil.needPushRedMapRainConfigMap.remove(config.getKeyid()+"" );
                           GroupRedBagRainUtil.updateConfigState(config.getKeyid(),2);
                           LogUtil.msgLog.info("RedBagRain|PushGroupRedBagRainConfigTask|OverPush|"+config.getKeyid());

                       }
                   },1000*10);
               }
           }
       }
    }
    public static  void  test(){
        {
            LocalDateTime time = LocalDateTime.now() ;
            DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00");
            String startDate = dtf2.format(time);
            LogUtil.msgLog.info("OneMin:PushGroupRedBagRainConfigTask");
            if( GroupRedBagRainUtil.needPushRedMapRainConfigMap.size()>0) {
                for ( GroupRedBagRainConfig config : GroupRedBagRainUtil.needPushRedMapRainConfigMap.values()) {
                    String pst = DateFormatUtils.format(config.getPushstarttime(), "yyyy-MM-dd HH:mm:00");
                    LocalDateTime time2 = LocalDateTime.parse(pst, dtf2);
                    //是否可推送
                    if ( config.getState() == 0||config.getState()==1) {
                        LogUtil.msgLog.info("PushGroupRedBagRainConfigTask|id=" + config.getKeyid() + "|" + pst);
                        long groupId = config.getGroupid();
                        int serverId = GameServerConfig.SERVER_ID;
                        List<GroupTable> tableList = GroupDao.getInstance().loadGroupPlayingTables(groupId, serverId);
                        if(tableList==null || tableList.size()==0){
                            continue;
                        }

                        //推送前 变更红包雨配置状态 1发放中
                        config.setState(1);
                        GroupRedBagRainUtil.needPushRedMapRainConfigMap.put(config.getKeyid()+"",config);
                       // GroupRedBagRainUtil.updateConfigState(config.getKeyid(),1);
                        if (null != tableList && tableList.size() > 0) {
                            for (GroupTable table : tableList) {
                                BaseTable table1 = TableManager.getInstance().getTable(table.getTableId());
                                if(table1==null){
                                    continue;
                                }
                                Map<Long, Player> playerMap = table1.getPlayerMap();
                                for (Player p : playerMap.values()) {
                                    if(p.isRobot()){
                                        continue;
                                    }
                                    p.writeComMessage(WebSocketMsgType.resp_code_GroupRedBagRain_push, config.getKeyid() + "", groupId + "");
                                }
                            }
                        }
                        //红包雨持续10S 10S后关闭抢的功能
//                   TaskExecutor.TIMER.schedule(new TimerTask() {
//                       @Override
//                       public void run() {
//                           System.out.println("指定延迟结束抢红包");
//                           config.setState(2);//结束
//                           GroupRedBagRainUtil.needPushRedMapRainConfigMap.remove(config.getKeyid()+"" );
//                           GroupRedBagRainUtil.updateConfigState(config.getKeyid(),2);
//                       }
//                   },1000*10);
                    }
                }
            }
        }
    }

   public static int getNextMinDelay(){
     int second =  LocalTime.now().getSecond();
     return (60-second)*1000;
   }
}
