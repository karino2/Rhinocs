package io.github.karino2.rhinocs;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.ArrayList;

import io.github.karino2.fastfile.FastFile;
import kotlin.Unit;


public class GlobalObject  extends ImporterTopLevel {
    private static final String[] TOP_COMMANDS = {
            "print",
            "read_key_callback",
            "select_open_file_callback",
            "select_new_file_callback",
            "select_open_dir_callback",
            "request_setup_package_root_dir",
            "open_dir",
            "create_uri",
            "show_toast",
            "load_gzip_skk_dictionary",
            "load_js_callback",
            "copy_to_clipboard",
            "current_clipboard",
            "query_text_dialog_callback",
            "enter_minibuffer",
            "leave_minibuffer",
            "eval_script",
    };

    public MainActivity activity;
    public RView rview;

    public void setup(MainActivity activity, RView rview) {
        this.activity = activity;
        this.rview = rview;
        defineProperty("activity", new JSActivityWrapper(activity), ScriptableObject.DONTENUM);
        defineProperty("rview", rview, ScriptableObject.DONTENUM);
    }
    public Rhinocs getRhinocs() { return rview.getRhinocs(); }
    public Window selectedWindow() { return getRhinocs().getSelectedWindow(); }
    Buffer selectedBuffer() { return getRhinocs().getSelectedBuffer(); }

    ArrayList<DelayedRequest> pendingRequestQueue;

    public void pushLoadRequest(String jsPath, Function onSuccess, Function onFailure) {
        pendingRequestQueue.add( DelayedRequest.Companion.jsLoadRequest(jsPath, onSuccess, onFailure) );
    }


    public boolean hasPendingRequest() { return !pendingRequestQueue.isEmpty(); }

    public DelayedRequest popDelayedRequest() {
        DelayedRequest first = pendingRequestQueue.get(0);
        pendingRequestQueue.remove(0);
        return first;
    }


    public GlobalObject(Context ctx) {
        pendingRequestQueue = new ArrayList<>();
        initStandardObjects(ctx, true);
        defineFunctionProperties(TOP_COMMANDS, GlobalObject.class, ScriptableObject.DONTENUM);
    }

    static GlobalObject getInstance(Function funcObj) {
        Scriptable scope = funcObj.getParentScope();
        while (scope != null && !(scope instanceof GlobalObject)) {
            scope = scope.getParentScope();
        }
        if (scope == null)
            throw new IllegalArgumentException("non GlobalObject func obj.");
        return (GlobalObject) scope;
    }

    private static String getFuncName(Function funcObj) {
        Object name = funcObj.get("name", funcObj);
        return name != Scriptable.NOT_FOUND ? Context.toString(name) : "function";
    }

    private static void verifyArgs(Function funcObj, Object[] args, Class<?>[] expectedTypes) {
        if (args.length != expectedTypes.length) {
            throw new IllegalArgumentException(getFuncName(funcObj) + " expects " + expectedTypes.length + " argument(s).");
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Class<?> type = expectedTypes[i];
            if (type == String.class) {
                if (arg == Context.getUndefinedValue() || arg == null) {
                    throw new IllegalArgumentException(getFuncName(funcObj) + " arg " + (i + 1) + " must be a string.");
                }
            } else if (type == Number.class) {
                if (!(arg instanceof Number)) {
                    throw new IllegalArgumentException(getFuncName(funcObj) + " arg " + (i + 1) + " must be a number.");
                }
            } else if (type != Object.class) {
                Object javaObj = Context.jsToJava(arg, type);
                if (javaObj == null || !type.isInstance(javaObj)) {
                    throw new IllegalArgumentException(getFuncName(funcObj) + " arg " + (i + 1) + " must be " + type.getSimpleName() + ".");
                }
            }
        }
    }

    public static Object read_key_callback(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class, Function.class, Function.class  });

        GlobalObject glob = getInstance(funcObj);

        String label = Context.toString(args[0]);
        Function onSuccess = (Function)args[1];
        Function onFailure = (Function)args[2];
        glob.pendingRequestQueue.add( new DelayedRequest(DelayedRequestType.READ_KEY,  new DelayedRequest.Arg(label, onSuccess, onFailure)));
        return Context.getUndefinedValue();
    }

    /*
        select_open_file_callback([mimeTypes], onSuccess, onFailure)
     */
    public static Object select_open_file_callback(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[]{ Scriptable.class, Function.class, Function.class });

        GlobalObject glob = getInstance(funcObj);

        Object[] mtypesObj = ctx.getElements((Scriptable) args[0]);
        Function onSuccess = (Function) args[1];
        Function onFailure = (Function) args[2];
        ArrayList<String> arr = new ArrayList<>();
        for (Object arg : mtypesObj) {
            arr.add(Context.toString(arg));
        }

        glob.pendingRequestQueue.add( new DelayedRequest(DelayedRequestType.SELECT_OPEN_FILE, new DelayedRequest.Arg(arr.toArray(new String[0]), onSuccess, onFailure)));
        return Context.getUndefinedValue();
    }

    /*
        select_new_file_callback(defaultName, onSuccess, onFailure)
     */
    public static Object select_new_file_callback(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[]{ String.class, Function.class, Function.class });

        GlobalObject glob = getInstance(funcObj);
        String defName = Context.toString(args[0]);
        Function onSuccess = (Function) args[1];
        Function onFailure = (Function) args[2];

        glob.pendingRequestQueue.add( new DelayedRequest(DelayedRequestType.SELECT_NEW_FILE, new DelayedRequest.Arg(defName, onSuccess, onFailure)));
        return Context.getUndefinedValue();
    }

    /*
        select_open_dir_callback(onSuccess, onFailure, uri=undefined)
     */
    public static Object select_open_dir_callback(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        if (args.length < 2)
            throw new IllegalArgumentException("No onSuccess, onFailure args.");

        GlobalObject glob = getInstance(funcObj);

        Function onSuccess = (Function) args[0];
        Function onFailure = (Function) args[1];
        String uri = null;
        if (args.length >= 3)
            uri = Context.toString(args[2]);

        glob.pendingRequestQueue.add( new DelayedRequest(DelayedRequestType.SELECT_OPEN_DIR, new DelayedRequest.Arg(uri, onSuccess, onFailure)));
        return Context.getUndefinedValue();
    }

    /*
        request_setup_package_root_dir()

        この関数は特別で、非同期にpackageのdirを選択してinit.jsなどをロードする。
        init.jsからさらに非同期に何かがloadされうるので、この終わりをpromiseで待つ事は出来ない事にする。
     */
    public static Object request_setup_package_root_dir(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        Function empty = new BaseFunction();
        glob.pendingRequestQueue.add( new DelayedRequest(DelayedRequestType.SETUP_PACKAGE_ROOT_DIR, new DelayedRequest.Arg(null, empty, empty)));
        return Context.getUndefinedValue();
    }

    /*
        FastFileを返す。
        FastFileのJSからのインターフェースとしては

        - getUri()
        - getName()
        - listFiles()
        - getLastModified()
        - getSize()
        - isDirectory()
        - isFile()

        などがある。
     */
    public static Object open_dir(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[]{String.class});
        GlobalObject glob = getInstance(funcObj);

        String uri = Context.toString(args[0]);
        return FastFile.Companion.fromTreeUri(glob.activity, Uri.parse(uri));
    }

    public static Object create_uri(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[]{String.class});
        GlobalObject glob = getInstance(funcObj);

        String uri = Context.toString(args[0]);
        return Context.javaToJS(Uri.parse(uri), glob);
    }

    public static Object print(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                builder.append(" ");
            builder.append(Context.toString(args[i]));
        }
        builder.append("\n");
        System.out.print(builder);
        getInstance(funcObj).getRhinocs().getLogBuffer().append(builder);
        return Context.getUndefinedValue();
    }

    public static Object load_gzip_skk_dictionary(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        String fname = Context.toString(args[0]);
        String content = glob.activity.readGZIPFileContent(fname);
        
        SkkDictionaryLoader skk = new SkkDictionaryLoader();
        return skk.parseData(ctx, funcObj.getParentScope(), content);
    }

    // load_jsは再入してしまうと以下のexceptionがでてしまったので、リクエストをpushするだけにして遅延ロードするようにする。
    // org.mozilla.javascript.WrappedException: Wrapped java.lang.IllegalStateException: Cannot have any pending top calls when executing a script with continuations
    //
    public static Object load_js_callback(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class, Function.class, Function.class });

        GlobalObject glob = getInstance(funcObj);
        String fname = Context.toString(args[0]);
        Function onSuccess = (Function)args[1];
        Function onFailure = (Function)args[2];
        glob.pushLoadRequest(fname, onSuccess, onFailure);
        return Context.getUndefinedValue();
    }

    public static Object show_toast(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        glob.activity.showMessage(Context.toString(args[0]));
        return Context.getUndefinedValue();
    }

    public static Object copy_to_clipboard(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        String text = Context.toString(args[0]);
        ClipboardManager clipboard = (ClipboardManager) glob.activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("rhinocs", text);
        clipboard.setPrimaryClip(clip);
        return Context.getUndefinedValue();
    }

    public static Object current_clipboard(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        ClipboardManager clipboard = (ClipboardManager) glob.activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).getText();
                return text != null ? text.toString() : "";
            }
        }
        return "";
    }

    /*
        query_text_dialog_callback(label, onSuccess, onFailure)
     */
    public static Object query_text_dialog_callback(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[]{ String.class, Function.class, Function.class });

        GlobalObject glob = getInstance(funcObj);

        String label = Context.toString(args[0]);
        Function onSuccess = (Function)args[1];
        Function onFailure = (Function)args[2];
        glob.pendingRequestQueue.add( new DelayedRequest(DelayedRequestType.QUERY_TEXT_DIALOG,  new DelayedRequest.Arg(label, onSuccess, onFailure)));
        return Context.getUndefinedValue();
    }

    /*(non-Javadoc)
     *
     * enter_minibuffer(prompt)
     *
     * promptを先頭に表示してミニバッファを作成してミニバッファウィンドウをアクティブにする
     *
     * @param {string} prompt
     */
    public static Object enter_minibuffer(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        String prompt = Context.toString(args[0]);
        MiniBufferWindow mwin = glob.getRhinocs().enterMiniBuffer(prompt);
        mwin.getMiniBuffer().getBuffer().setOnModified(()->{
            ScriptableObject hook = (ScriptableObject)glob.get("g_hooks", glob);
            Function runHook = (Function)hook.get("runHook", hook);
            String text = mwin.getMiniBuffer().getText();
            runHook.call(ctx, glob, hook, new Object[]{"minibuffer_modified_hook", text});
            return Unit.INSTANCE;
        });
        return Context.getUndefinedValue();
    }

    /*(non-Javadoc)
     *
     * leave_minibuffer()
     *
     * minibufferに入力されているテキストを返し、ミニバッファは破棄して最後にアクティブだったWindowをアクティブにする。
     *
     * @return {string}
     */
    public static Object leave_minibuffer(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        return glob.getRhinocs().leaveMiniBuffer();
    }

    public static Object eval_script(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[]{String.class});

        GlobalObject glob = getInstance(funcObj);
        String source = Context.toString(args[0]);
        return ctx.evaluateString(glob, source, "*eval*", 1, null);
    }

}
