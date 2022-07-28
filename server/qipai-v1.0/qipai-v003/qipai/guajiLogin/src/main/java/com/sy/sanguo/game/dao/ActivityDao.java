package com.sy.sanguo.game.dao;

import com.sy.sanguo.game.bean.Activity;
import com.sy.sanguo.game.bean.group.GroupRedBagRainConfig;
import com.sy.sanguo.game.bean.group.GroupRedBagResult;
import com.sy.sanguo.game.bean.group.GroupUser;
import com.sy.sanguo.game.pdkuai.db.dao.BaseDao;
import com.sy599.sanguo.util.SysPartitionUtil;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityDao extends BaseDao{
	private static ActivityDao _inst = new ActivityDao();

	public static ActivityDao getInstance() {
		return _inst;
	}

	public Activity getActivityById(int id) {
		try {
			return (Activity) getSql().queryForObject("activity.getActivityById", id);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<GroupRedBagRainConfig> loadNeedMasterAccountRedBagRainConfig(HashMap<String, Object> map) throws SQLException {
		return (List<GroupRedBagRainConfig>) getSql().queryForList("groupNew.loadNeedMasterAccount", map);

	}

	/**
	 * 没人抢得，可返回群主的红包
	 * @param configId
	 * @return
	 * @throws SQLException
	 */
	public List<GroupRedBagResult> loadAllUnTakeRedBag(long configId) throws SQLException {
		return (List<GroupRedBagResult>)getSql().queryForList("groupNew.loadAllUnTakeRedBag", configId);

	}

	/**
	 * 群成员未领取得红包
	 * @param configId
	 * @return
	 * @throws SQLException
	 */
	public List<GroupRedBagResult> loadAllUserUnTakeRedBag(long configId) throws SQLException {
		return (List<GroupRedBagResult>)getSql().queryForList("groupNew.loadAllUserUnTakeRedBag", configId);

	}


	public int updateRedBagResult(HashMap<String, Object> upResultState) throws SQLException {
		return getSql().update("groupNew.updateRedBagResult", upResultState);

	}

	public List<GroupRedBagRainConfig> loadNeedMemberAccountRedBagRainConfig(HashMap<String, Object> map) throws SQLException {
		return (List<GroupRedBagRainConfig>)getSql().queryForList("groupNew.loadNeedMemberAccountRedBagRainConfig", map);

	}

	/**
	 * 红包更新为用户已领取
	 * @param takeMap
	 * @return
	 * @throws SQLException
	 */
	public int updateUserTakeRedBag(HashMap<String, Object> takeMap) throws SQLException {
		return getSql().update("groupNew.updateUserTakeRedBagResult", takeMap);
	}


	public int updateGroupRedBagRainConfig(HashMap config) throws Exception {
		return   getSql().update("groupNew.updateRedBagConfigState",config);

	}
	public GroupUser loadGroupMaster(long groupId) throws Exception {
		Map<String, Object> map = new HashMap<>(8);
		map.put("groupId", groupId);
		return (GroupUser) getSql().queryForObject("groupNew.loadGroupMaster", map);
	}
	public int updateGroupUserCredit(HashMap<String, Object> map) throws Exception {
		return getSql().update("groupNew.updateGroupUserCredit", map);
	}

	public int insertGroupCreditLog(HashMap<String, Object> map) throws Exception {
		int res = 0;
		if(SysPartitionUtil.isWriteMaster()) {
			map.put("gpSeq", SysPartitionUtil.getGroupSeqForMaster(Long.valueOf(String.valueOf(map.get("groupId")))));
			res = getSql().update("groupNew.insertGroupCreditLog", map);
		}

		if (SysPartitionUtil.isWritePartition()) {
			map.put("gpSeq", SysPartitionUtil.getGroupSeqForPartition(Long.valueOf(String.valueOf(map.get("groupId")))));
			getSql().update("groupNew.insertGroupCreditLog", map);
		}
		return res;
	}

	public GroupUser loadGroupUser(long groupId, long userId) throws Exception {
		Map<String, Object> map = new HashMap<>(8);
		map.put("groupId", groupId);
		map.put("userId", userId);
		return (GroupUser) getSql().queryForObject("groupNew.loadGroupUser", map);
	}

}
