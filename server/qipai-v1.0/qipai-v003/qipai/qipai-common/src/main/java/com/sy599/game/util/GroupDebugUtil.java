package com.sy599.game.util;

import com.sy599.game.db.dao.BaseConfigDao;
import com.sy599.game.db.dao.TableCheckDao;
import com.sy599.game.db.enums.DbEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GM DEBUG  GROUP CHECK NEXT PLAYER GET CARD  ;IF PLAYER IN CONFIG WHITE NAME LIST;
 */
public class GroupDebugUtil {

    public static final Map<Long, List<Integer>> group_debug_playTypeConfig = new ConcurrentHashMap<>();
    public static final Map<Long, List<Long>> group_debug_playerWhiteNameList = new ConcurrentHashMap<>();

    public final static void init() {
        try {
            int gmIsOpen = ResourcesConfigsUtil.loadIntegerValue("GmDeBugConfig", "isOpen");
            if (gmIsOpen != 1) {
                group_debug_playTypeConfig.clear();
                group_debug_playerWhiteNameList.clear();
                return;
            }
            List<HashMap<String, Object>> list = TableCheckDao.getInstance().checkTableExists(DbEnum.LOGIN, "t_group_debug") ? BaseConfigDao.getInstance().loadGroupDedegConfig() : null;
            if (null == list) {
                return;
            }
            // SELECT groupid,playTypeList,whiteNameList FROM t_group_debug
            for (HashMap item : list) {
                long groupid = item.get("groupid") == null ? 0 : Long.parseLong(item.get("groupid").toString());
                String playTypeList = item.get("playTypeList") == null ? "0" : (String) item.get("playTypeList");
                String whiteNameList = item.get("whiteNameList") == null ? "0" : (String) item.get("whiteNameList");
                if (groupid > 0) {
                    group_debug_playTypeConfig.put(groupid, StringUtil.explodeToIntList(playTypeList));
                    group_debug_playerWhiteNameList.put(groupid, StringUtil.explodeToLongList(whiteNameList));
                }
            }
        } catch (Exception e) {
            LogUtil.errorLog.error("Exception:" + e.getMessage(), e);
        }
    }

    /**
     * 某群某玩法某成员是否开启debug
     * @param groupId
     * @param playType
     * @param userId
     * @return
     */
    public final static boolean groupMemberDebugPermission(long groupId, int playType, long userId,String ip) {
        if(groupId==0){
            return false;
        }
        if (null == group_debug_playTypeConfig || group_debug_playTypeConfig.isEmpty()) {
            return false;
        }
        if (null == group_debug_playerWhiteNameList || group_debug_playerWhiteNameList.isEmpty()) {
            return false;
        }
        if (group_debug_playTypeConfig.containsKey(groupId)) {
            List<Integer> playTypes = group_debug_playTypeConfig.get(groupId);
            if (playTypes.contains(playType)) {
                List<Long> players = group_debug_playerWhiteNameList.get(groupId);
                if (players.contains(userId)) {
                    //todo ipconfig

                    return true;
                }
            }
        }
        return false;
    }


}
