package io.github.karino2.rhinocs;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.ArrayList;
import java.util.List;


public class GlobalObject  extends ImporterTopLevel {
    private static final String[] TOP_COMMANDS = {
            "print",
            "read_key",
            "select_file",
            "open_uri",
            "forward_char",
            "backward_char",
            "forward_line",
            "backward_line",
            "insert",
            "default_self_insert_keys",
            "read_file",
            "write_file",
            "point",
            "delete_region",
            "save_buffer",
            "show_toast",
            "point_column",
            "set_goal_column",
            "goal_column",
            "goto_column",
            "point_max",
            "goto_char",
            "goto_eol",
            "goto_bol",
            "window_height",
            "scroll_window",
            "read_gzip_file",
            "load_gzip_skk_dictionary",
            "request_load_js",
            "get_buffer_create",
            "switch_to_buffer",
            "message",
            "mark_marker",
            "set_marker",
            "marker_position",
            "buffer_substring",
            "copy_to_clipboard",
            "current_clipboard",
            "put_pref_string",
            "get_pref_string",
            "query_text_dialog",
    };

    public static final int REQUEST_SELECT_FILE=1;
    public static final int REQUEST_READ_KEY=2;
    public static final int REQUEST_TEXT_DIALOG=3;

    public MainActivity activity;
    public RView rview;
    public Window getWindow() { return rview.getWindow(); }

    public ArrayList<String> loadRequestsQueue;

    public void pushLoadRequest(String jsPath) {
        loadRequestsQueue.add(jsPath);
    }

    public boolean hasPendingRequest() { return !loadRequestsQueue.isEmpty(); }
    public String popLoadRequest() {
        String first = loadRequestsQueue.get(0);
        loadRequestsQueue.remove(0);
        return first;
    }



    public GlobalObject(Context ctx) {
        loadRequestsQueue = new ArrayList<>();
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

    public static Object read_key(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        String msg = Context.toString(args[0]);
        try (Context cx = Context.enter()) {
            ContinuationPending pending = cx.captureContinuation();
            RequestArg ra = new RequestArg(REQUEST_READ_KEY, msg);
            pending.setApplicationState(ra);
            throw pending;
        }
    }

    public static Object select_file(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        try (Context cx = Context.enter()) {
            ArrayList<String> arr = new ArrayList<>();
            for (Object arg : args) {
                arr.add(Context.toString(arg));
            }
            ContinuationPending pending = cx.captureContinuation();
            RequestArg ra = new RequestArg(REQUEST_SELECT_FILE, arr.toArray(new String[0]));
            pending.setApplicationState(ra);
            throw pending;
        }
    }
    public static Object print(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                System.out.print(" ");
            System.out.print(Context.toString(args[i]));
        }
        System.out.print("\n");
        return Context.getUndefinedValue();
    }

    public static Object open_uri(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        String uriStr = Context.toString(args[0]);
        glob.openUri(Uri.parse(uriStr));
        return Context.getUndefinedValue();
    }
    public static Object forward_char(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        int delta = 1;
        if (args.length != 0)
            delta = (int) Context.toNumber(args[0]);
        glob.rview.getWindow().moveCharDelta(delta);
        return Context.getUndefinedValue();
    }

    public static Object backward_char(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        int delta = 1;
        if (args.length != 0)
            delta = (int) Context.toNumber(args[0]);
        glob.rview.getWindow().moveCharDelta(-delta);
        return Context.getUndefinedValue();
    }

    public static Object forward_line(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        int delta = 1;
        if (args.length != 0)
            delta = (int) Context.toNumber(args[0]);
        glob.rview.getWindow().moveLineDelta(delta);
        return Context.getUndefinedValue();
    }

    public static Object backward_line(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        int delta = 1;
        if (args.length != 0)
            delta = (int) Context.toNumber(args[0]);
        glob.rview.getWindow().moveLineDelta(-delta);
        return Context.getUndefinedValue();
    }

    public static Object insert(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        String content = Context.toString(args[0]);
        glob.getWindow().insert(content);
        return Context.getUndefinedValue();
    }

    public static Object default_self_insert_keys(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        List<String> strs = glob.rview.selfInsertKeys();
        return ctx.newArray(glob, strs.toArray());
    }

    public static Object read_file(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        String fname = Context.toString(args[0]);
        return glob.activity.readFileContent(fname);
    }

    // elispの (write-region start end filename) に合わせて wirte_file(content, path)にする。
    public static Object write_file(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class, String.class });

        GlobalObject glob = getInstance(funcObj);
        String content = Context.toString(args[0]);
        String path = Context.toString(args[1]);
        return glob.activity.writeFileContent(path, content);
    }

    public static Object read_gzip_file(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        String fname = Context.toString(args[0]);
        return glob.activity.readGZIPFileContent(fname);
    }

    public static Object load_gzip_skk_dictionary(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        String fname = Context.toString(args[0]);
        String content = glob.activity.readGZIPFileContent(fname);
        
        SkkDictionary skk = new SkkDictionary();
        return skk.parseData(ctx, funcObj.getParentScope(), content);
    }

    // load_jsは再入してしまうと以下のexceptionがでてしまったので、リクエストをpushするだけにして遅延ロードするようにする。
    // たぶんコンテキストとかをちゃんと使い回せば平気なんだろうけれど、その中からopen_fileなどのpending continuation系が呼ばれるとややこしいので。
    // org.mozilla.javascript.WrappedException: Wrapped java.lang.IllegalStateException: Cannot have any pending top calls when executing a script with continuations
    public static Object request_load_js(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        String fname = Context.toString(args[0]);
        glob.pushLoadRequest(fname);
        return Context.getUndefinedValue();
    }


    public static Object point(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        Point pt = glob.getWindow().getPoint();
        return pt.getPoint();
    }

    public static Object delete_region(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { Number.class, Number.class });

        GlobalObject glob = getInstance(funcObj);

        long p1 = (long)Context.toNumber(args[0]);
        long p2 = (long)Context.toNumber(args[1]);
        long start = Math.min(p1, p2);
        long end = Math.max(p1, p2);

        return glob.getWindow().deleteRegion(start, end);
    }

    public static Object save_buffer(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        return glob.getWindow().saveBuffer(glob.activity.getContentResolver());
    }

    public static Object show_toast(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        glob.activity.showMessage(Context.toString(args[0]));
        return Context.getUndefinedValue();
    }

    public static Object point_column(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { Number.class });

        GlobalObject glob = getInstance(funcObj);
        long pos = (long)Context.toNumber(args[0]);
        return glob.getWindow().pontToColumn(pos);
    }

    public static Object set_goal_column(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { Number.class });

        GlobalObject glob = getInstance(funcObj);
        int column = (int) Context.toNumber(args[0]);
        glob.getWindow().setGoalColumn(column);
        return Context.getUndefinedValue();
    }

    public static Object goal_column(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        return glob.getWindow().computeGoalGolumn();
    }

    public static Object goto_column(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { Number.class });

        GlobalObject glob = getInstance(funcObj);
        int column = (int) Context.toNumber(args[0]);
        return glob.getWindow().gotoColumn(column);
    }
    public static Object point_max(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        return glob.getWindow().getPointMax();
    }

    public static Object goto_char(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { Number.class });

        GlobalObject glob = getInstance(funcObj);
        long pos = (long) Context.toNumber(args[0]);
        glob.getWindow().gotoChar(pos);
        return Context.getUndefinedValue();
    }

    public static Object goto_bol(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        glob.getWindow().gotoBol();
        return Context.getUndefinedValue();
    }

    public static Object goto_eol(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        glob.getWindow().gotoEol();
        return Context.getUndefinedValue();
    }

    public static Object window_height(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        return glob.getWindow().getNumRows();
    }

    public static Object scroll_window(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { Number.class });

        GlobalObject glob = getInstance(funcObj);
        int delta = (int) Context.toNumber(args[0]);
        glob.getWindow().scrollWindow(delta);
        return Context.getUndefinedValue();
    }
    public void openUri(Uri uri) {
        rview.loadFile(activity.getContentResolver(), uri);
    }

    public static Object get_buffer_create(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        String bname = Context.toString(args[0]);
        Buffer buf = new Buffer();
        buf.setName(bname);
        return Context.javaToJS(buf, glob);
    }

    public static Object switch_to_buffer(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { Buffer.class });

        GlobalObject glob = getInstance(funcObj);
        //  || !(args[0] instanceof Buffer)
        Buffer buf = (Buffer)Context.jsToJava(args[0], Buffer.class);
        glob.getWindow().setBuffer(buf);
        return Context.getUndefinedValue();
    }

    public static Object message(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { String.class });

        GlobalObject glob = getInstance(funcObj);
        String msg = Context.toString(args[0]);
        glob.getWindow().setStatusText(msg);
        return Context.getUndefinedValue();
    }

    public static Object mark_marker(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        Marker mark = glob.getWindow().getBuffer().getMark();
        return Context.javaToJS(mark, glob);
    }

    public static Object set_marker(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { Marker.class, Number.class });

        GlobalObject glob = getInstance(funcObj);
        Marker marker = (Marker)Context.jsToJava(args[0], Marker.class);
        long pos = (long)Context.toNumber(args[1]);
        marker.setPosition(pos);
        return Context.getUndefinedValue();
    }

    public static Object marker_position(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { Marker.class });

        GlobalObject glob = getInstance(funcObj);
        Marker marker = (Marker)Context.jsToJava(args[0], Marker.class);
        return marker.getPosition();
    }

    Buffer getBuffer() { return getWindow().getBuffer(); }

    // 今の所第三引数（bufferオブジェクト）はサポートしないが、名前はbuffer_substringにしておく。
    public static Object buffer_substring(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[] { Number.class, Number.class });

        GlobalObject glob = getInstance(funcObj);
        long beg = (long)Context.toNumber(args[0]);
        long end = (long)Context.toNumber(args[1]);
        // 選択されてないケース。一応ここでもガードしておく。
        if (beg < 0 || end < 0)
            return "";
        return glob.getBuffer().substring(Math.min(beg, end), Math.max(beg, end));
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

    public static Object put_pref_string(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[]{String.class, String.class});

        GlobalObject glob = getInstance(funcObj);
        String key = Context.toString(args[0]);
        String value = Context.toString(args[1]);
        glob.activity.putPrefString(key, value);
        return Context.getUndefinedValue();
    }

    public static Object get_pref_string(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[]{String.class, String.class});

        GlobalObject glob = getInstance(funcObj);
        String key = Context.toString(args[0]);
        String defaultValue = Context.toString(args[1]);
        return glob.activity.getPrefString(key, defaultValue);
    }

    public static Object query_text_dialog(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        verifyArgs(funcObj, args, new Class<?>[]{String.class});

        try (Context cx = Context.enter()) {
            ContinuationPending pending = cx.captureContinuation();
            RequestArg ra = new RequestArg(REQUEST_TEXT_DIALOG, Context.toString(args[0]));
            pending.setApplicationState(ra);
            throw pending;
        }
    }
}
