package com.sunshinator.sharedchecklist;

public class Constants {

  public static final int MASK_RIGHT_SEE          = 0b00000001;
  public static final int MASK_RIGHT_ADD_ITEM     = 0b00000010;
  public static final int MASK_RIGHT_CHECK        = 0b00000100;
  public static final int MASK_RIGHT_CLEAN        = 0b00001000;
  public static final int MASK_RIGHT_ADD_USERS    = 0b00010000;
  public static final int MASK_RIGHT_REMOVE_USERS = 0b00100000;
  public static final int MASK_RIGHT_DELETE       = 0b01000000;
  public static final int MASK_RIGHT_ALL          = 0b11111111;

  public static final String FB_BASE_PATH = "SharedCheckList/lists";
}
