/**
 * (c) 2013 uchicom
 */
package com.uchicom.dirpop3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * 出力時に一度全てバッファに溜め込むので負荷があがってしまう。
 * 随時書き込むようにしないといけない。
 * @author uchicom: Shigeki Uchiyama
 *
 */
public class Pop3Handler implements Handler {

    /** 出力用の文字列バッファ */
    StringBuffer strBuff = new StringBuffer();
    /** ダイジェスト用の変数 */
    String timestamp;

    // ユーザーコマンドでユーザーが設定されたかどうかのフラグ
    /** ユーザー設定済みフラグ */
    boolean bUser;
    // 認証が許可されたかどうかのフラグ
    /** 認証済みフラグ */
    boolean bPass;
    
    /** ユーザー名 */
    String user;
    /** パスワード */
    String pass;
    /** ベースディレクトリ */
    File base;
    /** ユーザーメールボックス */
    File userBox;
    // メールbox内にあるメールリスト(PASSコマンド時に認証が許可されると設定される)
    /** メールボックス内のリスト */
    List<File> mailList;
    // DELEコマンド時に指定したメールが格納される(PASSコマンド時に認証が許可されると設定される)
    /** 削除リスト */
    List<File> delList;
    ByteBuffer readBuff = ByteBuffer.allocate(128);

    /**
     * 設定を保持するコンストラクタ.
     * @param base
     * @param hostName
     */
    public Pop3Handler(File base, String hostName) {
        this.base = base;
        timestamp = "<" +Thread.currentThread().getId() + "." + System.currentTimeMillis() + "@" + hostName + ">";
        strBuff.append(Pop3Static.RECV_OK);
        strBuff.append(timestamp);
        strBuff.append(Pop3Static.RECV_LINE_END);
    }
    /* (non-Javadoc)
     * @see com.uchicom.dirpop3.Handler#handle(java.nio.channels.SelectionKey)
     */
    @Override
    public void handle(SelectionKey key) throws IOException, NoSuchAlgorithmException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (key.isReadable()) {
            int length = channel.read(readBuff);
            if (length > 0 && checkCmd()) {
                String line = getCmd();
                if (line.matches(Pop3Static.REG_EXP_USER_NAME)) {
                    user(line);
                } else if (line.matches(Pop3Static.REG_EXP_PASS_WORD)) {
                    pass(line);
                } else if (line.matches(Pop3Static.REG_EXP_STAT)) {
                    stat(line);
                } else if (line.matches(Pop3Static.REG_EXP_LIST)) {
                    list(line);
                } else if (line.matches(Pop3Static.REG_EXP_LIST_NUM)) {
                    listNum(line);
                } else if (line.matches(Pop3Static.REG_EXP_RETR)) {
                    retr(line);
                } else if (line.matches(Pop3Static.REG_EXP_RETR_NUM)) {
                    retrNum(line);
                } else if (line.matches(Pop3Static.REG_EXP_DELE_NUM)) {
                    deleNum(line);
                } else if (line.matches(Pop3Static.REG_EXP_RSET)) {
                    rset(line);
                } else if (line.matches(Pop3Static.REG_EXP_QUIT)) {
                    quit(line);
                } else if (line.matches(Pop3Static.REG_EXP_NOOP)) {
                    noop(line);
                } else if (line.matches(Pop3Static.REG_EXP_TOP_NUM_NUM)) {
                    topNumNum(line);
                } else if (line.matches(Pop3Static.REG_EXP_UIDL)) {
                    uidl(line);
                } else if (line.matches(Pop3Static.REG_EXP_UIDL_NUM)) {
                    uidlNum(line);
                } else if (line.matches(Pop3Static.REG_EXP_APOP_NAME_DIGEST)) {
                    apopNameDigest(line);
                } else if ("".equals(line)) {
                    // 何もしない
                } else {
                    strBuff.append(Pop3Static.RECV_NG_CMD_NOT_FOUND);
                }
                if (strBuff.length() > 0) {
                    key.interestOps(SelectionKey.OP_WRITE);
                }
                readBuff.clear();
            }

        }
        if (key.isWritable() && strBuff.length() > 0) {
            //初回の出力を実施
            channel.write(ByteBuffer.wrap(strBuff.toString().getBytes()));
            //書き込み処理が終わっていないかを確認する。
            //処理が途中の場合は途中から実施する。
            strBuff.setLength(0);
            key.interestOps(SelectionKey.OP_READ);
        }
    }
    
    /**
     * コマンド行が入力されたかどうかチェックする.
     * @return
     */
    public boolean checkCmd() {
        String line = new String(Arrays.copyOfRange(readBuff.array(), 0, readBuff.position()));
        return line.indexOf("\r\n") >= 0;
    }
    /**
     * コマンド行から文字列を取得する.
     * \r\nは除外する.
     * @return
     */
    public String getCmd() {
        String line = new String(Arrays.copyOfRange(readBuff.array(), 0, readBuff.position()));
        return line.substring(0, line.indexOf("\r\n"));
    }
    
    /**
     * USERコマンド.
     * @param line
     */
    public void user(String line) {
        bUser = true;
        user = line.split(" ")[1];
        strBuff.append(Pop3Static.RECV_OK_LINE_END);
    }
    
    /**
     * PASS コマンド.
     * @param line
     * @throws IOException
     */
    public void pass(String line) throws IOException {
        if (bUser && !bPass) {
            pass = line.split(" ")[1];
            // ユーザーチェック
            boolean existUser = false;
            for (File box : base.listFiles()) {
                if (box.isDirectory()) {
                    if (user.equals(box.getName())) {
                        userBox = box;
                        File[] mails = userBox
                                .listFiles(new FilenameFilter() {

                                    @Override
                                    public boolean accept(
                                            File dir,
                                            String name) {
                                        File file = new File(
                                                dir, name);
                                        if (file.isFile()
                                                && !file.isHidden()
                                                && file.canRead()
                                                && !Pop3Static.PASSWORD_FILE_NAME
                                                        .equals(name)) {
                                            return true;
                                        }
                                        return false;
                                    }

                                });

                        mailList = Arrays.asList(mails);
                        Collections.sort(mailList, FileComparator.instance);
                        delList = new ArrayList<File>();
                        
                        existUser = true;
                    }
                }
            }
            if (existUser) {
                // パスワードチェック
                if (!"".equals(pass)) {
                    File passwordFile = new File(userBox,
                            Pop3Static.PASSWORD_FILE_NAME);
                    if (passwordFile.exists()
                            && passwordFile.isFile()) {
                        BufferedReader passReader = new BufferedReader(
                                new InputStreamReader(
                                        new FileInputStream(
                                                passwordFile)));
                        String password = passReader
                                .readLine();
                        while ("".equals(password)) {
                            password = passReader
                                    .readLine();
                        }
                        passReader.close();
                        if (pass.equals(password)) {
                            strBuff.append(Pop3Static.RECV_OK_LINE_END);
                            bPass = true;
                        } else {
                            // パスワード不一致エラー
                            strBuff.append(Pop3Static.RECV_NG_LINE_END);
                        }
                    } else {
                        // パスワードファイルなしエラー
                        strBuff.append(Pop3Static.RECV_NG_LINE_END);
                    }
                } else {
                    // パスワード入力なしエラー
                    strBuff.append(Pop3Static.RECV_NG_LINE_END);
                }
            } else {
                // ユーザー存在しないエラー
                strBuff.append(Pop3Static.RECV_NG_LINE_END);
            }
        } else {
            // ユーザー名未入力エラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }

    /**
     * STATコマンド.
     * @param line
     */
    public void stat(String line) {
        if (bPass) {
            // 簡易一覧表示
            strBuff.append(Pop3Static.RECV_OK);
            long fileLength = 0;
            int fileCnt = 0;
            for (File child : mailList) {
                if (!delList.contains(child)) {
                    fileLength += child.length();
                    fileCnt++;
                }
            }
            strBuff.append(fileCnt);
            strBuff.append(" ");
            strBuff.append(fileLength);
            strBuff.append(Pop3Static.RECV_LINE_END);
        } else {
            // 認証なしエラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }
    /**
     * LIST コマンド.
     * @param line
     */
    public void list(String line) {
        if (bPass) {
            // リスト表示
            strBuff.append(Pop3Static.RECV_OK_LINE_END);
            for (int i = 0; i < mailList.size(); i++) {
                File child = mailList.get(i);
                if (!delList.contains(child)) {
                    strBuff.append(i + 1);
                    strBuff.append(' ');
                    strBuff.append(child.length());
                    strBuff.append(Pop3Static.RECV_LINE_END);
                }
            }
            strBuff.append(Pop3Static.RECV_DATA_END);
        } else {
            // 認証なしエラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }
    /**
     * LIST メッセージ番号 コマンド.
     * @param line
     */
    public void listNum(String line) {
        if (bPass) {
            // 指定番号のリスト表示
            String[] lines = line.split(" ");
            int index = Integer.parseInt(lines[1]) - 1;
            if (0 <= index && index < mailList.size()) {
                File child = mailList.get(index);
                if (!delList.contains(child)) {
                    strBuff.append(Pop3Static.RECV_OK);
                    strBuff.append(line.substring(5));
                    strBuff.append(' ');
                    strBuff.append(child.length());
                    strBuff.append(Pop3Static.RECV_LINE_END);
                } else {
                    strBuff.append(Pop3Static.RECV_NG_LINE_END);
                }
            } else {
                // index範囲外
                strBuff.append(Pop3Static.RECV_NG_LINE_END);
            }
        } else {
            // 認証なしエラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }
    /**
     * RETR コマンド.
     * @param line
     * @throws IOException
     */
    public void retr(String line) throws IOException {
        if (bPass) {
            strBuff.append(Pop3Static.RECV_OK_LINE_END);
            for (File child : mailList) {
                if (!delList.contains(child)) {
                    BufferedReader passReader = new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(
                                            child)));
                    String readLine = passReader.readLine();
                    while (readLine != null) {
                        strBuff.append(readLine);
                        strBuff.append(Pop3Static.RECV_LINE_END);
                        
                        readLine = passReader.readLine();
                    }
                    passReader.close();
                }
            }
            strBuff.append(Pop3Static.RECV_DATA_END);
        } else {
            // エラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }
    /**
     * RETR メッセージ番号コマンド.
     * @param line
     * @throws IOException
     */
    public void retrNum(String line) throws IOException {
        if (bPass) {
            String[] lines = line.split(" ");
            int index = Integer.parseInt(lines[1]) - 1;
            if (0 <= index && index < mailList.size()) {
                File child = mailList.get(index);
                if (!delList.contains(child)) {
                    strBuff.append(Pop3Static.RECV_OK);
                    strBuff.append(child.length());
                    strBuff.append(Pop3Static.RECV_LINE_END);
                    BufferedReader passReader = new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(
                                            child)));
                    String readLine = passReader.readLine();
                    while (readLine != null) {
                        strBuff.append(readLine);
                        strBuff.append(Pop3Static.RECV_LINE_END);
                        
                        readLine = passReader.readLine();
                    }
                    strBuff.append(Pop3Static.RECV_DATA_END);
                    passReader.close();
                } else {
                    strBuff.append(Pop3Static.RECV_NG_LINE_END);
                }
            } else {
                // index範囲外
                strBuff.append(Pop3Static.RECV_NG_LINE_END);
            }
        } else {
            // エラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }
    
    /**
     * DELE コマンド.
     * @param line
     */
    public void deleNum(String line) {
        if (bPass) {
            // 削除処理
            String[] lines = line.split(" ");
            int index = Integer.parseInt(lines[1]) - 1;
            if (0 <= index && index < mailList.size()) {
                File child = mailList.get(index);
                delList.add(child);
                strBuff.append(Pop3Static.RECV_OK_LINE_END);
            } else {
                // index範囲外
                strBuff.append(Pop3Static.RECV_NG_LINE_END);
            }
        } else {
            // エラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }
    /**
     * RSET コマンド.
     * @param line
     */
    public void rset(String line) {
     // リセット
        if (bPass) {
            // 消去マークを無くす
            delList.clear();
            strBuff.append(Pop3Static.RECV_OK_LINE_END);
        } else {
            // エラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }
    /**
     * QUIT コマンド.
     * @param line
     */
    public void quit(String line) {
        if (delList != null) {
            // 消去マークの入ったファイルを削除する
            for (File delFile : delList) {
                delFile.delete();
            }
        }
        strBuff.append(Pop3Static.RECV_OK_LINE_END);
    }
    /**
     * NOOP コマンド.
     */
    public void noop(String line) {

        // 何もしない
    }
    /**
     * TOP メッセージ番号 行数 コマンド.
     * @param line
     * @throws IOException
     */
    public void topNumNum(String line) throws IOException {
        if (bPass) {
            // TRANSACTION 状態でのみ許可される
            String[] lines = line.split(" ");
            int index = Integer.parseInt(lines[1]) - 1;
            if (0 <= index && index < mailList.size()) {
                File child = mailList.get(index);
                if (!delList.contains(child)) {
                    strBuff.append(Pop3Static.RECV_OK_LINE_END);
                    BufferedReader passReader = new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(
                                            child)));
                    String readLine = passReader.readLine();
                    int maxRow = Integer.parseInt(lines[2]);
                    int row = 0;
                    boolean head = true;
                    while (readLine != null && (head || row < maxRow)) {
                        strBuff.append(readLine);
                        strBuff.append(Pop3Static.RECV_LINE_END);
                        
                        readLine = passReader.readLine();
                        if (!head) {
                            row++;
                        }
                        if (!"".equals(readLine)) {
                            head = false;
                        }
                    }
                    strBuff.append(Pop3Static.RECV_DATA_END);
                    passReader.close();
                } else {
                    strBuff.append(Pop3Static.RECV_NG_LINE_END);
                }
            } else {
                strBuff.append(Pop3Static.RECV_NG_LINE_END);
            }
        } else {
            // 認証なしエラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }
    /**
     * UIDL コマンド.
     * @param line
     */
    public void uidl(String line) {
        if (bPass) {
            // TRANSACTION 状態でのみ許可される
            strBuff.append(Pop3Static.RECV_OK_LINE_END);
            for (int i = 0; i < mailList.size(); i++) {
                File child = mailList.get(i);
                if (!delList.contains(child)) {
                    strBuff.append(i + 1);
                    strBuff.append(' ');
                    String name = child.getName();
                    int lastIndex = name.lastIndexOf('~');
                    if (lastIndex < 0) {
                        if (name.length() > 70) {
                            lastIndex = name.length() - 70;
                        } else {
                            strBuff.append(name);
                        }
                    } else {
                        strBuff.append(name.substring(lastIndex));
                    }
                    strBuff.append(Pop3Static.RECV_LINE_END);
                }
            }
            strBuff.append(Pop3Static.RECV_DATA_END);
        } else {
            // 認証なしエラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }

    /**
     * UIDL メッセージ番号 コマンド.
     * @param line
     */
    public void uidlNum(String line) {
        if (bPass) {
            // TRANSACTION 状態でのみ許可される
            String[] lines = line.split(" ");
            int index = Integer.parseInt(lines[1]) - 1;
            if (0 <= index && index < mailList.size()) {
                File child = mailList.get(index);
                if (!delList.contains(child)) {
                    strBuff.append(Pop3Static.RECV_OK);
                    strBuff.append(lines[1]);
                    strBuff.append(' ');
                    String name = child.getName();
                    int lastIndex = name.lastIndexOf('~');
                    if (lastIndex < 0) {
                        if (name.length() > 70) {
                            lastIndex = name.length() - 70;
                        } else {
                            strBuff.append(name);
                        }
                    } else {
                        strBuff.append(name.substring(lastIndex));
                    }
                    strBuff.append(Pop3Static.RECV_LINE_END);
                } else {
                    strBuff.append(Pop3Static.RECV_NG_LINE_END);
                }
            } else {
                // index範囲外
                strBuff.append(Pop3Static.RECV_NG_LINE_END);
            }
        } else {
            // 認証なしエラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }
    
    /**
     * APOP ユーザー名 ダイジェスト コマンド.
     * @param line
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public void apopNameDigest(String line) throws IOException, NoSuchAlgorithmException {
        if (!bPass) {
            String[] lines = line.split(" ");
            user = lines[1];
            String digest = lines[2];
            // ユーザーチェック
            boolean existUser = false;
            for (File box : base.listFiles()) {
                if (box.isDirectory()) {
                    if (user.equals(box.getName())) {
                        userBox = box;
                        File[] mails = userBox
                                .listFiles(new FilenameFilter() {
                                    @Override
                                    public boolean accept(
                                            File dir,
                                            String name) {
                                        File file = new File(
                                                dir, name);
                                        if (file.isFile()
                                                && !file.isHidden()
                                                && !Pop3Static.PASSWORD_FILE_NAME
                                                        .equals(name)) {
                                            return true;
                                        }
                                        return false;
                                    }
                                });
                        mailList = Arrays.asList(mails);
                        Collections.sort(mailList, FileComparator.instance);
                        delList = new ArrayList<File>();
                        existUser = true;
                    }
                }
            }
            if (existUser) {
                // パスワードチェック
                File passwordFile = new File(userBox,
                        Pop3Static.PASSWORD_FILE_NAME);
                if (passwordFile.exists()
                        && passwordFile.isFile()) {
                    BufferedReader passReader = new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(
                                            passwordFile)));
                    String password = passReader.readLine();
                    while ("".equals(password)) {
                        password = passReader.readLine();
                    }
                    passReader.close();
                    // ダイジェストとタイムスタンプを元にダイジェストを作成
                    MessageDigest md = MessageDigest
                            .getInstance("MD5");
                    md.update((timestamp + password)
                            .getBytes());
                    byte[] passBytes = md.digest();
                    StringBuffer strBuff = new StringBuffer(
                            32);
                    for (int i = 0; i < passBytes.length; i++) {
                        int d = passBytes[i] & 0xFF;
                        if (d < 0x10) {
                            strBuff.append("0");
                        }
                        strBuff.append(Integer
                                .toHexString(d));
                    }
                    if (digest.equals(strBuff.toString())) {
                        strBuff.append(Pop3Static.RECV_OK_LINE_END);
                        bPass = true;
                    } else {
                        // パスワード不一致エラー
                        strBuff.append(Pop3Static.RECV_NG_LINE_END);
                    }
                } else {
                    // パスワードファイルなしエラー
                    strBuff.append(Pop3Static.RECV_NG_LINE_END);
                }
            } else {
                // ユーザー存在しないエラー
                strBuff.append(Pop3Static.RECV_NG_LINE_END);
            }
        } else {
            // パスワード認証後に再度パスワード認証はエラー
            strBuff.append(Pop3Static.RECV_NG_LINE_END);
        }
    }
}