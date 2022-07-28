package com.sy599.game.qipai.hbgzp.rule;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sy599.game.qipai.hbgzp.tool.HbgzpTool;
import com.sy599.game.util.JacksonUtil;

/**
 * 判断牌型
 * 
 * @author lc
 * 
 */
public class HbgzpCardIndexArr {
	HbgzpIndex[] a = new HbgzpIndex[5];

	public void addPaohzCardIndex(int count, List<Hbgzp> majiangList, int val) {
		if (a[count] == null) {
			a[count] = new HbgzpIndex();
		}
		a[count].addPaohz(val, majiangList);
		a[count].addVal(val);
	}

	/**
	 * 根据牌的张数得到牌
	 * 
	 * @param size
	 *            张数
	 * @return
	 */
	public Map<Integer, List<Hbgzp>> getPaohzCardMap(int size) {
		Map<Integer, List<Hbgzp>> map = new HashMap<>();
		for (int i = 0; i < a.length; i++) {
			if (size <= i + 1) {
				HbgzpIndex majiangIndex = a[i];
				if (majiangIndex != null) {
					map.putAll(majiangIndex.getPaohzValMap());
				}
			}

		}
		return map;
	}


	/**
	 * 牌的张数大于2的 (对子数)
	 * 
	 * @return
	 */
	public int getDuiziNum() {
		int num = 0;
		for (int i = 1; i < a.length; i++) {
			HbgzpIndex majiangIndex = a[i];
			if (majiangIndex == null) {
				continue;
			}
			if (i == 3) {
				num += majiangIndex.getLength() * 2;
			} else {
				num += majiangIndex.getLength();

			}
		}
		return num;
	}

	/**
	 * 牌的张数大于3的 (刻字数)
	 * 
	 * @return
	 */
	public int getKeziNum() {
		int num = 0;
		for (int i = 2; i < a.length; i++) {
			HbgzpIndex majiangIndex = a[i];
			if (majiangIndex == null) {
				continue;
			}
			num += majiangIndex.getLength();

		}
		return num;
	}

	public List<Hbgzp> getKeziList() {
		List<Hbgzp> list = new ArrayList<>();
		for (int i = 2; i < a.length; i++) {
			HbgzpIndex majiangIndex = a[i];
			if (majiangIndex == null) {
				continue;
			}
			list.addAll(majiangIndex.getPaohzList());

		}
		return list;
	}

	/**
	 * 得到牌
	 * 
	 * @param index
	 *            0一张 , 1 二张 , 2 三张 , 3 四张
	 * @return
	 */
	public HbgzpIndex getPaohzCardIndex(int index) {
		HbgzpIndex majiangIndex = a[index];
		if (majiangIndex == null) {
			// return new PaohzCardIndex();
		}
		return majiangIndex;
	}

	public HbgzpIndex[] getA() {
		return a;
	}

	public String tostr() {
		int i = 0;
		String str = "";
		for (HbgzpIndex majiang : a) {
			if (majiang == null) {
				continue;
			}
			str += i + "  " + JacksonUtil.writeValueAsString(majiang.getValList()) + " -->" + JacksonUtil.writeValueAsString(majiang.getPaohzValMap()) + "\n";
			i++;
		}
		return str;

	}
}
