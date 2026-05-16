package io.github.karino2.rhinocs;

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
            "select_file",
            "open_uri",
            "forward_char",
            "backward_char",
            "forward_line",
            "backward_line",
            "insert",
            "default_self_insert_keys",
            "read_file",
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
    };

    public static final int REQUEST_SELECT_FILE=1;

    public MainActivity activity;
    public RView rview;
    public Window getWindow() { return rview.getWindow(); }

    public GlobalObject(Context ctx) {
        initStandardObjects(ctx, true);
        defineFunctionProperties(TOP_COMMANDS, GlobalObject.class, ScriptableObject.DONTENUM);
    }

    static GlobalObject getInstance(Function funcObj) {
        Scriptable scope = funcObj.getParentScope();
        if (!(scope instanceof GlobalObject))
            throw new IllegalArgumentException("non GlobalObject func obj.");
        return (GlobalObject) scope;
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
        GlobalObject glob = getInstance(funcObj);
        if (args.length != 1)
            throw new IllegalArgumentException("non uri argument.");
        String uriStr = Context.toString(args[0]);
        glob.openUri(Uri.parse(uriStr));
        return Context.getUndefinedValue();
    }
    public static Object forward_char(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        int delta = 1;
        if (args.length != 0)
            delta = (int) Context.toNumber(args[0]);
        glob.rview.getWindow().forwardChar(delta);
        return Context.getUndefinedValue();
    }

    public static Object backward_char(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        int delta = 1;
        if (args.length != 0)
            delta = (int) Context.toNumber(args[0]);
        glob.rview.getWindow().backwardChar(delta);
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
        GlobalObject glob = getInstance(funcObj);
        if (args.length != 1)
            throw new IllegalArgumentException("non insert argument.");
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
        GlobalObject glob = getInstance(funcObj);
        if (args.length != 1)
            throw new IllegalArgumentException("non file name argument.");
        String fname = Context.toString(args[0]);
        return glob.activity.readFileContent(fname);
    }

    public static Object point(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        Point pt = glob.getWindow().getPoint();
        return pt.getPoint();
    }

    public static Object delete_region(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        if (args.length != 2)
            throw new IllegalArgumentException("invalid delete region argument num.");

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
        GlobalObject glob = getInstance(funcObj);
        if (args.length != 1)
            throw new IllegalArgumentException("show_toast must be 1 arg.");
        glob.activity.showMessage(Context.toString(args[0]));
        return Context.getUndefinedValue();
    }

    public static Object point_column(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        if (args.length != 1)
            throw new IllegalArgumentException("point_column must be 1 arg.");
        long pos = (long)Context.toNumber(args[0]);
        return glob.getWindow().pontToColumn(pos);
    }

    public static Object set_goal_column(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        if (args.length != 1)
            throw new IllegalArgumentException("set_goal_column must be 1 arg.");
        int column = (int) Context.toNumber(args[0]);
        glob.getWindow().setGoalColumn(column);
        return Context.getUndefinedValue();
    }

    public static Object goal_column(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        return glob.getWindow().computeGoalGolumn();
    }

    public static Object goto_column(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        if (args.length != 1)
            throw new IllegalArgumentException("goto_column must be 1 arg.");
        int column = (int) Context.toNumber(args[0]);
        return glob.getWindow().gotoColumn(column);
    }
    public static Object point_max(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        return glob.getWindow().getPointMax();
    }

    public static Object goto_char(Context ctx, Scriptable thisObj, Object[] args, Function funcObj) {
        GlobalObject glob = getInstance(funcObj);
        if (args.length != 1)
            throw new IllegalArgumentException("goto_char must be 1 arg.");
        long pos = (long) Context.toNumber(args[0]);
        glob.getWindow().gotoChar(pos);
        return Context.getUndefinedValue();
    }
    public void openUri(Uri uri) {
        rview.loadFile(activity.getContentResolver(), uri);
    }
}
