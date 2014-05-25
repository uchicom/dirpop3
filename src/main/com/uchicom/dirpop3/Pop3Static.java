/**
 * (c) 2012 uchicom
 */
package com.uchicom.dirpop3;

import java.io.File;

/**
 * POP3に関する定数クラス
 * @author Uchiyama Shigeki
 * 
 */
public class Pop3Static {
    
    /** デバッグフラグ */

    public static final boolean DEBUG = true;

	// POP3返却メッセージ
	/** 返却メッセージ(+OK) */
	public static String RECV_OK = "+OK ";
	/** 返却メッセージ(+OK(行終端文字列あり)) */
	public static String RECV_OK_LINE_END =  "+OK\r\n";
	/** 返却メッセージ(-NG) */
	public static String RECV_NG =  "-NG ";
	/** 返却メッセージ(-NG(行終端文字列あり)) */
	public static String RECV_NG_LINE_END =  "-NG\r\n";
	/** 返却メッセージ(-NG(コマンド未対応)) */
	public static String RECV_NG_CMD_NOT_FOUND =  "-NG Command not support\r\n";
	/** 返却メッセージ(-ERR(行終端文字列あり) */
	public static String RECV_ERR_LINE_END =  "-ERR\r\n";
	/** 返却メッセージ(行終端文字列) */
	public static String RECV_LINE_END = "\r\n";
	/** 返却メッセージ(.(行終端文字列あり)) */
	public static String RECV_DATA_END =  ".\r\n";

	// POP3コマンド正規表現
	/** USER 文字の正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_USER_NAME = "^[Uu][Ss][Ee][Rr] +[^ ]+ *$";
	/** PASS 文字の正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_PASS_WORD = "^[Pp][Aa][Ss][Ss] +[^ ]+ *$";
	/** STATの正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_STAT = "^[Ss][Tt][Aa][Tt] *$";
	/** LISTの正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_LIST = "^[Ll][Ii][Ss][Tt] *$";
	/** LIST 数値の正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_LIST_NUM = "^[Ll][Ii][Ss][Tt] +[1-9][0-9]* *$";
	/** RETRの正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_RETR = "^[Rr][Ee][Tt][Rr] *$";
	/** RETR 数値の正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_RETR_NUM = "^[Rr][Ee][Tt][Rr] +[1-9][0-9]* *$";
	/** DELE 数値の正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_DELE_NUM = "^[Dd][Ee][Ll][Ee] +[1-9][0-9]* *$";
	/** RSETの正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_RSET = "^[Rr][Ss][Ee][Tt] *$";
	/** QUITの正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_QUIT = "^[Qq][Uu][Ii][Tt] *$";
	/** NOOPの正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_NOOP = "^[Nn][Oo][Oo][Pp] *$";
	/** TOP 数値 数値の正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_TOP_NUM_NUM = "^[Tt][Oo][Pp] +[1-9][0-9]* +[0-9]+ *$";
	/** UIDLの正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_UIDL = "^[Uu][Ii][Dd][Ll] *$";
	/** UIDL 数値の正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_UIDL_NUM = "^[Uu][Ii][Dd][Ll] +[1-9][0-9]* *$";
	/** APOP 文字 文字の正規表現(大文字小文字後続スペース) */
	public static String REG_EXP_APOP_NAME_DIGEST = "^[Aa][Pp][Oo][Pp] +[^ ]+ +[^ ]+ *$";
	/** CAPAの正規表現(大文字小文字後続スペース) */
    public static String REG_EXP_CAPA = "^[Cc][Aa][Pp][Aa] *$";
	
	
	//POP3 STARTTLS拡張コマンド
	/** STLSの正規表現(大文字小文字後続スペース) */
    public static String REG_EXP_STLS = "^[Ss][Tt][Ll][Ss] *$";

	/** 日時書式 */
	public static String DATE_TIME_MILI_FORMAT = "yyyyMMdd_HHmmss.SSS";

	/** パスワードファイルのパス */
	public static String PASSWORD_FILE_NAME = "pass.txt";
	
	//初期設定
	/** デフォルトメールボックスディレクトリ */
    public static File DEFAULT_MAILBOX = new File("mailbox");
    /** デフォルト待ち受けポート番号 */
    public static int DEFAULT_PORT = 8110;
    /** デフォルト接続待ち数 */
	public static int DEFAULT_BACK = 10;
	/** デフォルトスレッドプール数 */
	public static int DEFAULT_POOL = 10;
}