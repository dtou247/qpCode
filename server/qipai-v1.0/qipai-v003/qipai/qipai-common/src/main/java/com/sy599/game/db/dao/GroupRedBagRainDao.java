package com.sy599.game.db.dao;

import com.sy599.game.db.bean.redBagRain.GroupRedBagRainConfig;
import com.sy599.game.db.bean.redBagRain.GroupRedBagResult;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/**
 * creatBy butao
 * date: 2021/5/22 0022
 * desc:
 */
public class GroupRedBagRainDao extends  BaseDao{

    private static GroupRedBagRainDao _inst = new GroupRedBagRainDao();

    public static GroupRedBagRainDao getInstance() {
        return _inst;
    }

    public List<GroupRedBagRainConfig> loadAllConfigs(HashMap<String, Object> param) throws SQLException {
        //加载今天的红包雨 配置
        return (List<GroupRedBagRainConfig>) this.getSqlLoginClient().queryForList("redbagInfo.loadAllConfig",param);

    }

    public List<GroupRedBagRainConfig> loadAllOverConfigs(HashMap<String, Object> param) throws SQLException {
       //加载今日结束  且需要清理结算 的红包雨配置
        return (List<GroupRedBagRainConfig>) this.getSqlLoginClient().queryForList("redbagInfo.loadAllOverConfig",param);

    }

    public int updateConfigState(HashMap<String, Object> param) throws SQLException {
        return this.getSqlLoginClient().update("redbagInfo.updateRedBagConfigState",param);
    }

    public List<GroupRedBagResult> loadAllUnTakeRedBag(long keyId) throws SQLException {
        return (List<GroupRedBagResult> )this.getSqlLoginClient().queryForList("redbagInfo.loadAllUnTakeRedBag",keyId);
    }

    public int updateResultState(HashMap<String, Object> upResultState) throws SQLException {
      return  this.getSqlLoginClient().update("redbagInfo.updateResultState",upResultState);
    }

    public int getOneRedBag(HashMap<String, Object> map) throws SQLException {
        return  this.getSqlLoginClient().update("redbagInfo.getOneRedBagResult",map);
    }

    public GroupRedBagResult getRedBagNum(HashMap<String, Object> map) throws SQLException {
        return (GroupRedBagResult) this.getSqlLoginClient().queryForObject("redbagInfo.getRedBagNum",map);
    }

    public int updateHuiZhangRecordResultState(HashMap<String, Object> map) throws SQLException {
        return  this.getSqlLoginClient().update("redbagInfo.updateHuiZhangRecordResultByState",map);
    }
}
