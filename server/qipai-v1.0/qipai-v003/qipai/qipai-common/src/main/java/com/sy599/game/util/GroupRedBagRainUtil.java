package com.sy599.game.util;


import com.sy599.game.character.Player;
import com.sy599.game.db.bean.activityRecord.RedBagConfig;
import com.sy599.game.db.bean.redBagRain.GroupRedBagRainConfig;
import com.sy599.game.db.dao.GroupRedBagRainDao;
import com.sy599.game.db.dao.TableCheckDao;
import com.sy599.game.db.dao.group.GroupDao;
import com.sy599.game.db.enums.DbEnum;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * creatBy butao
 * date: 2021/5/20 0020
 * desc:亲友全抢红包
 */
public class GroupRedBagRainUtil {
    /**
     * 红包雨配置
     */
    public static final Map<String, GroupRedBagRainConfig> needPushRedMapRainConfigMap = new ConcurrentHashMap<>();
    public static final Map<String, GroupRedBagRainConfig> needAccountRedMapRainConfigMap = new ConcurrentHashMap<>();

    public final static void initRedBagRainConfigs() {
        try {
            if (
                TableCheckDao.getInstance().checkTableExists(DbEnum.LOGIN, "t_group_redbagrain_config")
            ) {
                LocalDateTime time = LocalDateTime.now().plusMinutes(-1);
                DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00");
                String startDate = dtf2.format(time);
                String endDate = LocalDate.now().toString() + " 23:59:59";
                HashMap<String,Object> param = new HashMap<>();
                param.put("startDate",startDate);
                param.put("endDate",endDate);
                List<GroupRedBagRainConfig> list = GroupRedBagRainDao.getInstance().loadAllConfigs(param);
                List<GroupRedBagRainConfig> overlist = GroupRedBagRainDao.getInstance().loadAllOverConfigs(param);
                if(null!=list && list.size()>0){
                    for (GroupRedBagRainConfig r:list ) {
                        needPushRedMapRainConfigMap.put(r.getKeyid()+"",r);
                    }
                }
                if(null!=overlist && overlist.size()>0){
                    for (GroupRedBagRainConfig r:overlist ) {
                        needAccountRedMapRainConfigMap.put(r.getKeyid()+"",r);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.errorLog.info("GroupRedBagRainUtil.initRedBagRainConfigs|error|", e);
        }
    }

    /**
     * 更新信用分状态
     * @param keyid
     * @param state
     */
    public static void updateConfigState(long keyid, int state) {
        HashMap<String,Object> param = new HashMap<>();
        param.put("keyId",keyid);
        param.put("state",state);// 0开始 1发放 2结束
        try {
            GroupRedBagRainDao.getInstance().updateConfigState(param);
        } catch (SQLException e) {
            LogUtil.errorLog.info("GroupRedBagRainUtil.updateConfigState|error|", e);
        }
    }

    /**
     * 红包雨信用分日志
     * @param groupId
     * @param userId
     * @param credit
     */
    public static void addCreditLog(long groupId,long userId,long credit){
        try {
            HashMap<String, Object> logDest = new HashMap<>();
            //结算给群玩家
            logDest.put("optUserId", userId);
            logDest.put("userId", userId);
            //信用分日志
            logDest.put("groupId", groupId);
            logDest.put("tableId", 0);
            logDest.put("credit", credit);
            logDest.put("type", 6);//红包雨来源
            logDest.put("flag", 1);
            logDest.put("userGroup", -1 );
            logDest.put("mode", 1);
            logDest.put("promoterId1", 0);
            logDest.put("promoterId3", 0);
            logDest.put("promoterId4", 0);
            logDest.put("promoterId5", 0);
            logDest.put("promoterId6", 0);
            logDest.put("promoterId7", 0);
            logDest.put("promoterId8", 0);
            logDest.put("promoterId9", 0);
            logDest.put("promoterId10", 0);
            logDest.put("createdTime", new Date());
            logDest.put("roomName", "福利雨");
            logDest.put("groupTableId", 0);
            //玩家日志
            GroupDao.getInstance().insertGroupCreditLog(logDest);
        }catch (Exception e){
            LogUtil.errorLog.info("GroupRedBagRainUtil.addCreditLog|error|", e);
        }
    }

}
