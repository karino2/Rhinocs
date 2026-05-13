package io.github.karino2.rhinocs;

import android.net.Uri;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.ArrayList;

public class GlobalObject  extends ImporterTopLevel {
    private static final String[] TOP_COMMANDS = {
            "print",
            "select_file",
            "open_uri"
    };

    public static final int REQUEST_SELECT_FILE=1;

    public MainActivity activity;
    public RView rview;

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
            throw new IllegalArgumentException("non GlobalObject func obj.");
        String uriStr = Context.toString(args[0]);
        glob.openUri(Uri.parse(uriStr));
        return Context.getUndefinedValue();
    }

    public void openUri(Uri uri) {
        rview.loadFile(activity.getContentResolver(), uri);
    }
}
