package com.dctp.bgylib.view;

public interface Pullable {
	public static final int BOTH = 0;
	public static final int TOP = 1;
	public static final int BOTTOM = 2;
	public static final int NONE = -1;

	/**
	 * 判断是否可以下拉，如果不需要下拉功能可以直接return false
	 * 
	 * @return true如果可以下拉否则返回false
	 */
	boolean canPullDown();

	/**
	 * 判断是否可以上拉，如果不需要上拉功能可以直接return false
	 * 
	 * @return true如果可以上拉否则返回false
	 */
	boolean canPullUp();
}
