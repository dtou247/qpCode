package com.sy599.game.gcommand.com;

import com.sy599.game.base.BaseTable;
import com.sy599.game.character.Player;
import com.sy599.game.common.constant.LangMsg;
import com.sy599.game.gcommand.BaseCommand;
import com.sy599.game.msg.serverPacket.ComMsg;
import com.sy599.game.util.LangHelp;
import com.sy599.game.websocket.netty.coder.MessageUnit;

/**
 * 前端请求洗牌
 */
public class XipaiCommand extends BaseCommand {

	@Override
	public void execute(Player player, MessageUnit message) throws Exception {

		BaseTable table = player.getPlayingTable();
		if (table == null) {
			player.writeErrMsg(LangHelp.getMsg(LangMsg.code_1));
			return;
		}

		if(!table.isXipai()){
			player.writeErrMsg(LangMsg.code_265);
			return;
		}
		if(!table.checkXipaiCreditOnStartNext(player)){
			player.writeErrMsg(LangMsg.code_266);
			return;
		}
		ComMsg.ComReq req = (ComMsg.ComReq) this.recognize(ComMsg.ComReq.class, message);
		if(req!=null){
			if(	req.getParamsCount()==1){
				int beForeReadyXipai =req.getParams(0);
				if(beForeReadyXipai==1){
					player.setXiPaiReady(1);
					table.addXipaiName(player.getName());
					return;
				}
			}

		}

//		player.setXipaiStatus(1);
		//扣除洗牌分 改成洗牌准备 开局后扣分
		//table.calcCreditXipai(player);
		table.addXipaiName(player.getName());
		player.setXiPaiReady(1);
//		ComMsg.ComRes.Builder com = SendMsgUtil.buildComRes(WebSocketMsgType.res_code_xipai,player.getName());
//		for (Player tableplayer : table.getSeatMap().values()) {
//			tableplayer.writeSocket(com.build());
//		}


//		player.setXipaiStatus(0);
//		player.setXipaiCount(player.getXipaiCount()+1);
//		player.setXipaiScore(player.getXipaiScore()+table.getXipaiScoure());
	}

	@Override
	public void setMsgTypeMap() {
	}
}
