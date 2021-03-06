package cn.ieclipse.smartqq.console;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.wb.swt.SWTResourceManager;

import com.scienjus.smartqq.QNUploader;
import com.scienjus.smartqq.QNUploader.AuthInfo;
import com.scienjus.smartqq.QNUploader.UploadInfo;
import com.scienjus.smartqq.model.Discuss;
import com.scienjus.smartqq.model.DiscussMessage;
import com.scienjus.smartqq.model.Friend;
import com.scienjus.smartqq.model.Group;
import com.scienjus.smartqq.model.GroupMessage;
import com.scienjus.smartqq.model.Message;

import cn.ieclipse.smartqq.QQPlugin;
import cn.ieclipse.smartqq.Utils;
import cn.ieclipse.smartqq.preferences.QiniuPerferencePage;
import cn.ieclipse.smartqq.views.ContactView;

public class ChatConsole extends IOConsole {
    private static final String ENTER_KEY = "\r\n";
    private String id;
    private Friend f;
    private Group g;
    private Discuss d;
    private ChatConsolePage page;
    
    public ChatConsole(String name, String consoleType,
            ImageDescriptor imageDescriptor, String encoding,
            boolean autoLifecycle) {
        super(name, consoleType, imageDescriptor, encoding, autoLifecycle);
    }
    
    public ChatConsole(String name, ImageDescriptor imageDescriptor) {
        super(name, imageDescriptor);
    }
    
    public ChatConsole(Friend f) {
        this(f.getMarkname(), null);
        this.id = "F_" + f.getUserId();
        this.f = f;
    }
    
    public ChatConsole(Group f) {
        this(f.getName(), null);
        this.id = ("G_" + f.getId());
        this.g = f;
    }
    
    public ChatConsole(Discuss f) {
        this(f.getName(), null);
        this.id = ("D_" + f.getId());
        this.d = f;
    }
    
    public static ChatConsole create(Object obj) {
        if (obj instanceof Friend) {
            return new ChatConsole((Friend) obj);
        }
        else if (obj instanceof Group) {
            return new ChatConsole((Group) obj);
        }
        else if (obj instanceof Discuss) {
            return new ChatConsole((Discuss) obj);
        }
        return null;
    }
    
    public static boolean isChatConsole(IConsole existing, Object obj) {
        if (existing instanceof ChatConsole) {
            ChatConsole console = ((ChatConsole) existing);
            String id = console.id;
            String name = existing.getName();
            
            if (obj instanceof Friend) {
                return ("F_" + ((Friend) obj).getUserId()).equals(id);
            }
            else if (obj instanceof Group) {
                return ("G_" + ((Group) obj).getId()).equals(id);
            }
            else if (obj instanceof Discuss) {
                return ("D_" + ((Discuss) obj).getId()).equals(id);
            }
        }
        return false;
    }
    
    // public ChatConsoleOutputStream newMessageStream() {
    // return new ChatConsoleOutputStream(this);
    // }
    
    @Override
    public IPageBookViewPage createPage(IConsoleView view) {
        IPageBookViewPage page = null;
        page = this.page = new ChatConsolePage(this, view);
        return page;
    }
    
    public void writeMessage(Message message) {
    
    }
    
    public void writeMessage(GroupMessage message) {
    
    }
    
    public void writeMessage(DiscussMessage message) {
    
    }
    
    public void write(String msg) {
        try {
            outputStream.write(msg + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void writeMine(String input) {
        String msg = String.format("%s %s: %s",
                new SimpleDateFormat("HH:mm:ss").format(new Date()), "me",
                input);
        try {
            mineStream.write(msg);
            mineStream.write('\n');
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void post(final String msg) {
        if (this.f != null && id.startsWith("F_")) {
            QQPlugin.getDefault().getClient().sendMessageToFriend(f.getUserId(),
                    msg);
        }
        else if (this.g != null && id.startsWith("G_")) {
            QQPlugin.getDefault().getClient().sendMessageToGroup(g.getId(),
                    msg);
        }
        else if (this.d != null && id.startsWith("D_")) {
            QQPlugin.getDefault().getClient().sendMessageToDiscuss(d.getId(),
                    msg);
        }
    }
    
    public void sendFile(final String file) {
        final File f = new File(file);
        new Thread() {
            public void run() {
                try {
                    QNUploader uploader = QQPlugin.getDefault().getUploader();
                    IPreferenceStore store = QQPlugin.getDefault()
                            .getPreferenceStore();
                    String ak = store.getString(QiniuPerferencePage.AK);
                    String sk = store.getString(QiniuPerferencePage.SK);
                    String bucket = store.getString(QiniuPerferencePage.BUCKET);
                    String domain = store.getString(QiniuPerferencePage.DOMAIN);
                    String qq = QQPlugin.getDefault().getClient()
                            .getAccountInfo().getAccount();
                    boolean enable = store
                            .getBoolean(QiniuPerferencePage.ENABLE);
                    boolean ts = store.getBoolean(QiniuPerferencePage.TS);
                    if (!enable) {
                        ak = "";
                        sk = "";
                    }
                    UploadInfo info = uploader.upload(qq, f, ak, sk, bucket,
                            null);
                    String url = info.getUrl(domain, ts);
                    
                    String msg = String.format(
                            "来自SmartQQ的文件: %s (大小%s), 点击链接%s查看",
                            Utils.getName(file),
                            Utils.formatFileSize(info.fsize), url);
                    writeMine(msg);
                    post(msg);
                } catch (Exception e) {
                    error("发送失败：" + e.getMessage());
                }
            };
        }.start();
        
    }
    
    public String readLine() {
        Scanner scanner = new Scanner(getInputStream(), getEncoding());
        String result;
        try {
            result = scanner.nextLine();
        } catch (NoSuchElementException endOfFile) {
            result = null;
        }
        return result;
    }
    
    private IOConsoleInputStream inputStream;
    private IOConsoleOutputStream errorStream;
    private IOConsoleOutputStream outputStream;
    private IOConsoleOutputStream promptStream;
    private IOConsoleOutputStream mineStream;
    
    @Override
    protected void init() {
        super.init();
        inputStream = getInputStream();
        outputStream = newOutputStream();
        errorStream = newOutputStream();
        promptStream = newOutputStream();
        mineStream = newOutputStream();
        
        outputStream.setColor(SWTResourceManager.getColor(SWT.COLOR_BLACK));
        inputStream.setColor(SWTResourceManager.getColor(SWT.COLOR_BLUE));
        promptStream.setColor(
                SWTResourceManager.getColor(SWT.COLOR_WIDGET_DARK_SHADOW));
        mineStream.setColor(SWTResourceManager.getColor(SWT.COLOR_DARK_BLUE));
        errorStream.setColor(SWTResourceManager.getColor(SWT.COLOR_RED));
        
        // new Thread(inputRunnable).start();
    }
    
    private Runnable inputRunnable = new Runnable() {
        
        @Override
        public void run() {
            String input = null;
            do {
                try {
                    promptStream.write(">>");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                input = readLine();
                if (input != null) {
                    writeMine(input);
                    post(input);
                }
            } while (input != null && !page.getControl().isDisposed());
        }
    };
    
    // public ISchedulingRule getSchedulingRule() {
    // return new ChatSchedulingRule();
    // }
    //
    // class ChatSchedulingRule implements ISchedulingRule {
    //
    // @Override
    // public boolean contains(ISchedulingRule rule) {
    // return rule == this;
    // }
    //
    // @Override
    // public boolean isConflicting(ISchedulingRule rule) {
    // if (contains(rule)) {
    // return true;
    // }
    // if (rule != this && rule instanceof ChatSchedulingRule) {
    // boolean ret = (((ChatSchedulingRule) rule)
    // .getConsole() == ChatConsole.this);
    // return ret;
    // }
    // if (rule == this && rule instanceof ChatSchedulingRule) {
    // return getConsole().lock;
    // }
    // return false;
    // }
    //
    // public ChatConsole getConsole() {
    // return ChatConsole.this;
    // }
    // }
    public void hide() {
        toggleContactView(false);
        clearConsole();
    }
    
    public void close() {
        toggleContactView(false);
        QQPlugin.getDefault().closeAllChat();
    }
    
    public void toggleHide() {
        if (QQPlugin.getDefault().enable) {
            hide();
            QQPlugin.getDefault().enable = false;
        }
        else {
            toggleContactView(true);
            QQPlugin.getDefault().enable = true;
        }
    }
    
    public void toggleClose() {
        if (QQPlugin.getDefault().enable) {
            close();
            QQPlugin.getDefault().enable = false;
        }
        else {
            toggleContactView(true);
            QQPlugin.getDefault().enable = true;
        }
    }
    
    private void toggleContactView(boolean show) {
        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
        if (show) {
            try {
                page.showView(ContactView.ID);
            } catch (PartInitException e) {
                write(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        IViewPart view = page.findView(ContactView.ID);
        if (view != null) {
            page.hideView(view);
        }
    }
    
    public void activeInput() {
        final StyledText text = getPage().getViewer().getTextWidget();
        Shell pshell = text.getShell();
        int x = 0;
        int y = text.getBounds().height;
        Control p = text.getParent();
        while (p != pshell) {
            x += p.getLocation().x;
            y += p.getLocation().y;
            p = p.getParent();
        }
        InputShell shell = InputShell.getInstance(pshell);
        shell.setConsole(this);
        shell.setLocation(x, y);
        shell.open();
        shell.layout();
        shell.setVisible(true);
    }
    
    public TextConsolePage getPage() {
        return page;
    }
    
    public void error(Throwable e) {
        e.printStackTrace(new PrintStream(errorStream));
    }
    
    public void error(String msg) {
        try {
            errorStream.write(msg);
            errorStream.write('\n');
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
