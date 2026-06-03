/*
  hook 関連

  既存のhook
  - minibuffer_modified_hook
  - enter_minibuffer_hook
  - exit_minibuffer_hook
  - visit_newfile_hook(uri, fname)
*/
function RunHook() {
  this.hooks = [];
}

RunHook.prototype.runAll = function(args) {
  for(let hook of this.hooks) {
    hook.apply(null, args);
  }
}

RunHook.prototype.add = function(hook) {
  this.hooks.push(hook);
}

RunHook.prototype.remove = function(hook) {
  this.hooks = this.hooks.filter((h) => h !== hook);
}



let g_hooks = {
  hookMap: {},
  addHook(name, hook) {
    if(!this.hookMap[name])
      this.hookMap[name] = new RunHook();

    this.hookMap[name].add(hook);
  },
  removeHook(name, hook) {
    if(!this.hookMap[name]) return;
    this.hookMap[name].remove(hook);
  },
  runHook(name, ...args) {
    if(!this.hookMap[name]) return;
    this.hookMap[name].runAll(args);
  }
};

/*
  KeyMap関連。
*/

function KeyMap() {
  this.keyMap = {};
}

KeyMap.prototype.defineKey = function(keySeq, func) {
  if (typeof keySeq === 'string') {
    this.keyMap[keySeq] = func;
    return;
  }

  let current = this.keyMap;
  for(let i = 0; i < keySeq.length-1; i++) {
    let k = keySeq[i];
    let v = current[k];
    if (!v || typeof v !== 'object') current[k] = {}

    current = current[k];
  }
  current[keySeq[keySeq.length-1]] = func
}

KeyMap.prototype.removeKey = function(keySeq) {
  if (typeof keySeq === 'string') {
    delete this.keyMap[keySeq];
    return;
  }

  let current = this.keyMap;
  for (let i = 0; i < keySeq.length-1; i++) {
    let k = keySeq[i];
    if (!current || typeof current !== 'object' || !(k in current)) return;
    current = current[k];
  }

  if (current && typeof current === 'object') {
    delete current[keySeq[keySeq.length-1]];
  }
}


KeyMap.prototype.lookupLastMap = function(keySeq) {
  if(keySeq.length == 0)
    return this.keyMap;

  let current = this.keyMap;
  for (let i = 0; i < keySeq.length; i++) {
    if (!current || typeof current !== 'object') return undefined;
    current = current[keySeq[i]];
  }
  return current;
}

KeyMap.prototype.defineDefaultSelfInsert = function() {
    let defSelfKeys = default_self_insert_keys();
    defSelfKeys.forEach(k => this.keyMap[k] = self_insert);
}

KeyMap.prototype.cloneDict = function(kmapDict) {
  let newDict = {};
  for(let k in kmapDict) {
    let v = kmapDict[k];
    if (typeof v === 'function') {
      newDict[k] = v;
    }
    else if (v !== null && typeof v === 'object') {
      newDict[k] = this.cloneDict(v);
    }
  }
  return newDict;
}

KeyMap.prototype.clone = function() {
  let newMap = new KeyMap();
  newMap.keyMap = this.cloneDict(this.keyMap);
  return newMap;
}


function CreateKeyMapStack(iniKeyMap) {
  return {
    keyMapStack: [iniKeyMap],

    currentKeyMap() {
      return this.keyMapStack[this.keyMapStack.length - 1];
    },

    pushKeyMap(keymap) {
      this.keyMapStack.push(keymap);
    },

    popKeyMap() {
      if (this.keyMapStack.length > 1) {
        this.keyMapStack.pop();
      }
    },
    length() { return this.keyMapStack.length; },
    getKeyMap(i) { return this.keyMapStack[i]; },
  };
}


/*
  ミニバッファ用のキーマップを指定してenterする処理とleaveする処理の共通処理。
*/
function enter_minibuffer_common(miniKeyMap, prompt) {
  g_keyMapHandler.isMiniBuffer = true
  g_keyMapHandler.pushKeyMap(miniKeyMap);
  enter_minibuffer(prompt);
  g_hooks.runHook("enter_minibuffer_hook")
}

function leave_minibuffer_common() {
  g_keyMapHandler.popKeyMap();
  g_keyMapHandler.isMiniBuffer = false;
  g_hooks.runHook("exit_minibuffer_hook");
  return leave_minibuffer();
}

/*
  FilteringList
  ミニバッファを用いた絞り込みリスト
*/
function read_filtering_list(candidates) {
  let filtering =  {
    candidates: candidates,
    filtered: candidates.slice(),
    doFilter(text) {
      if(text === "")
        this.filtered = this.candidates;
      else
        this.filtered = this.candidates.filter((item)=> -1 != item.indexOf(text));
    },
    shown() {
      if(this.matchExists())
        return this.filtered;
      return ["No mathing results"];
    },
    matchExists(){ return this.filtered.length > 0 },
    selected(index) {
      if(this.matchExists())
        return this.filtered[index];
      return undefined;
    }
  };

  let flist = get_floating_list();
  flist.items = candidates;

  let onModified = (text)=> {
    filtering.doFilter(text);
    flist.items = filtering.shown();
  };
  g_hooks.addHook("minibuffer_modified_hook", onModified);

  let leave = ()=> {
    g_hooks.removeHook("minibuffer_modified_hook", onModified);
    leave_minibuffer_common();
  }

  let miniKeyMap = new KeyMap();
  miniKeyMap.defineKey("C-p", ()=>flist.moveUp());
  miniKeyMap.defineKey("Up", ()=>flist.moveUp());
  miniKeyMap.defineKey("C-n", ()=>flist.moveDown());
  miniKeyMap.defineKey("Down", ()=>flist.moveDown());
  let promise = new Promise((resolve, reject)=> {
    miniKeyMap.defineKey("Return", ()=> {
      leave();
      let res = filtering.selected(flist.selectedIndex);
      flist.clear();
      if(res) {
        resolve(res);
      } else {
        reject();
      }
    });
    miniKeyMap.defineKey("C-g", ()=> {
      leave();
      reject();
    });
  });

  enter_minibuffer_common(miniKeyMap, "Filgtering: ");
  return promise;
}

(function() {

let targetBuf;
let targetWin;
let orgPos;
let lastMatchPos;
let lastMatchText;

let gotoChar = (pos) => {
  targetWin.gotoChar(pos);
  lastMatchPos = pos;
};

let newSearchForward = (text) => {
    let res = targetBuf.searchForward(orgPos, text);
    if (res) {
      gotoChar(res);
    } else {
      let wrapped = targetBuf.searchForward(0, text, orgPos);
      if (wrapped) {
        gotoChar(wrapped);
      } else {
        gotoChar(orgPos);
        message("No matches");
      }
    }
  };

let searchNextForward = () => {
    if (lastMatchText === "") return;

    let res = targetBuf.searchForward(lastMatchPos + 1, lastMatchText);
    if (res) {
      gotoChar(res);
    } else {
      let wrapped = targetBuf.searchForward(0, lastMatchText, lastMatchPos);
      if (wrapped) {
        if (wrapped == lastMatchPos) {
          message("No more matches");
          return;
        }
        gotoChar(wrapped);
        message("Wrapped");
      } else {
        message("No matches");
      }
    }
  };

let newSearchBackward = (text) => {
    let res = targetBuf.searchBackward(orgPos, text);
    if (res) {
      gotoChar(res);
    } else {
      let wrapped = targetBuf.searchBackward(targetBuf.getPositionMax(), text, orgPos);
      if (wrapped) {
        gotoChar(wrapped);
      } else {
        gotoChar(orgPos);
        message("No matches");
      }
    }
  };

let searchNextBackward = () => {
    if (lastMatchText === "") return;

    let res = targetBuf.searchBackward(lastMatchPos - 1, lastMatchText);
    if (res) {
      gotoChar(res);
    } else {
      let wrapped = targetBuf.searchBackward(targetBuf.getPositionMax(), lastMatchText, lastMatchPos);
      if (wrapped) {
        if (wrapped == lastMatchPos) {
          message("No more matches");
          return;
        }
        gotoChar(wrapped);
        message("Wrapped");
      } else {
        message("No matches");
      }
    }
};

function isearch_common(newSearch) {
  targetBuf = selected_buffer();
  targetWin = selected_window();
  orgPos = point();
  lastMatchPos = orgPos;
  lastMatchText = "";
  targetWin.setDrawCaret(true);

  let onModified = (text) => {
    lastMatchText = text;
    if (text === "") {
      gotoChar(orgPos);
      return;
    }
    newSearch(text);
  };

  g_hooks.addHook("minibuffer_modified_hook", onModified);
  let leave = ()=> {
    targetWin.setDrawCaret(false);
    g_hooks.removeHook("minibuffer_modified_hook", onModified);
    leave_minibuffer_common();
  }
  
  let commitResult = () => {    
    leave();
  };
  let cancelResult = () => {
    gotoChar(orgPos);
    leave();
  }
  let miniKeyMap = new KeyMap();
  miniKeyMap.defineKey("C-r", searchNextBackward);
  miniKeyMap.defineKey("C-s", searchNextForward);
  miniKeyMap.defineKey("Return", commitResult);
  miniKeyMap.defineKey("C-g", cancelResult);
  enter_minibuffer_common(miniKeyMap, "isearch: ");
}


function isearch_forward() {
  isearch_common(newSearchForward);
}

function isearch_backward() {
  isearch_common(newSearchBackward);
}

global.isearch_forward = isearch_forward;
global.isearch_backward = isearch_backward;
})();

/*
コマンド
*/

function self_insert() {
   insert($key);
}

function delete_backward_char(n=1) {
   let end = point();
   backward_char(n);
   delete_region(point(), end);
}

function delete_char(n=1) {
   let beg = point();
   forward_char(n);
   delete_region(beg, point());
}

function query_text_dialog(label) {
  return new Promise((resolve, reject)=> {
    query_text_dialog_callback(label, resolve, reject);
  });
}

function read_key(label) {
  return new Promise((resolve, reject)=> {
    read_key_callback(label, resolve, reject);
  });
}

function select_open_file(mimeTypes) {
  return new Promise((resolve, reject)=> {
    select_open_file_callback(mimeTypes, resolve, reject);
  });
}

function select_new_file(defName) {
  return new Promise((resolve, reject)=> {
    select_new_file_callback(defName, resolve, reject);
  });
}

function find_file() {
  select_open_file(["*/*"]).then(([uri, fname])=> {
    open_uri(uri);
    g_hooks.runHook("visit_newfile_hook", uri, fname);
  })
}

function new_file() {
  let buf = generate_new_buffer("Untitled");
  set_buffer(buf);
}

function saveBuffer() {
  let buf = selected_buffer();
  if(buf.url)
  {
    save_buffer();
    message("Saved!");
  } else {
    select_new_file(buf.name)
    .then(([uri, fname])=> {
      if(set_buffer_url(buf, uri)){
        save_buffer();
        message("Saved!");
        g_hooks.runHook("visit_newfile_hook", uri, fname);
      }
    })
  }
}

function next_line(delta=1) {
    let goal = goal_column();
    forward_line(delta);
    goto_column(goal);
    set_goal_column(goal);
}

function previous_line(delta=1) {
    let goal = goal_column();
    backward_line(delta);
    goto_column(goal);
    set_goal_column(goal);
}

function beginning_of_buffer() {
  goto_char(0)
}

function end_of_buffer() {
  goto_char(point_max());
 }

function next_page() {
  scroll_window(Math.floor(window_height()-2))
}

function previous_page() {
  scroll_window(-Math.floor(window_height()-2))
}

function beginning_of_line() { goto_bol(); }
function end_of_line() { goto_eol(); }

function set_mark_command() {
  let marker = mark_marker();
  set_marker(marker, point());
  message("mark set");
}

function exchange_point_and_mark() {
  let marker = mark_marker();
  let mark = marker_position(marker);
  set_marker(marker, point());
  goto_char(mark);
}

function withRegion(f) {
  let beg = point();
  let end = marker_position(mark_marker());
  if (end < 0) {
    message("No mark set");
    return;
  }
  return f(Math.min(beg, end), Math.max(beg, end));
}

function eval_region() {
  withRegion((beg, end) => {
    let text = buffer_substring(beg, end);
    let res = eval_script(text);

    // ミニバッファに行く関数をevalした時とかは結果を挿入しない。
    if (is_minibuffer())
      return;

    insert("\n");
    let out = "";
    try {
      if (res !== null && res !== undefined) out = String(res);
    } catch (e) {
      out = "";
    }
    insert(out);
    insert("\n");
  });
}

function copy_region() {
  withRegion((beg, end) => {
    let text = buffer_substring(beg, end);
    copy_to_clipboard(text);
    message("Copied");
  });
}

function kill_region_inner(beg, end) {
    let text = buffer_substring(beg, end);
    copy_to_clipboard(text);
    delete_region(beg, end);
}

function kill_region() {
  withRegion((beg, end) => {
    kill_region_inner(beg, end);
  });
}

function kill_line() {
  let beg = point();
  if (is_eol()) {
    forward_line(1);
  } else {
    goto_eol();
  }
  let end = point();
  if (end > beg) {
    kill_region_inner(beg, end);
  }
}

function yank() {
  let text = current_clipboard();
  insert(text);
}

function set_device_id(devid) {
  put_pref_string("device_id", devid);
  message("set: " + devid);
}

function get_device_id() {
  return get_pref_string("device_id", "Default");
}

function get_per_device_storage() {
  return `/storage/per_device/${get_device_id()}/`;
}

function join_path(...parts) {
  if (parts.length === 0) return "";
  return parts.reduce((acc, part, idx) => {
    part = String(part);
    if (idx === 0) return part;
    if (!acc.endsWith("/")) acc += "/";
    if (part.startsWith("/")) part = part.slice(1);
    return acc + part;
  }, "");
}

function load_js(fname) {
  return new Promise((resolve, reject)=> {
    load_js_callback(fname, resolve, reject);
  });
}

function is_minibuffer() {
  return selected_buffer().isMiniBuffer();
}

function global_set_key(keyPat, func) {
    g_keyMapHandler.keyMapStack.currentKeyMap().defineKey(keyPat, func);
}

/*
minibufferのキーマップにセット
*/
function global_mini_set_key(keyPat, func) {
    g_keyMapHandler.miniKeyMapStack.currentKeyMap().defineKey(keyPat, func);
}

function read_string(prompt) {
  let miniKeyMap = new KeyMap();
  let promise = new Promise((resolve, reject)=> {
    miniKeyMap.defineKey("Return", ()=> {
      let ret = leave_minibuffer_common();
      resolve(ret);
    });
    miniKeyMap.defineKey("C-g", ()=> {
      leave_minibuffer_common();
      reject();
    });
  });

  enter_minibuffer_common(miniKeyMap, prompt);
  return promise;
}

function execute_extended_command() {
  read_string("M-x ").then((cmd)=> {
    global[cmd]();
  });
}

function switch_to_buffer() {
  let bufs = buffer_list();
  let names = bufs.map(b=>b.name);
  read_filtering_list(names)
     .then(n=> {
      let index = names.indexOf(n);
      let buf = bufs[index];
      set_buffer(buf);
     });
}


/*
  isearchなど複雑なものはglobal.isearch_forwardとかを使うので、hoistされないから最後にCreateDefaultKeyMapとその呼出を持ってくる。
*/
let g_keyMapHandler = {
  lastKeySequence: [],
  delegateRequest: false,
  isMiniBuffer: false,
  keyMapStack: CreateKeyMapStack(CreateDefaultKeyMap()),
  miniKeyMapStack: CreateKeyMapStack(CreateDefaultMiniBufferKeyMap()),

  handleOneKeyMap(keymap, str) {
    let lmap = keymap.lookupLastMap(this.lastKeySequence);
    if (lmap) {
      let v = lmap[str];
      if (typeof v === 'function') {
        this.lastKeySequence.length = 0;
        v();
        if (this.delegateRequest) {
          this.delegateRequest = false;
          return false;
        }
        return true;
      }
      if (v !== null && typeof v === 'object') {
        this.lastKeySequence.push(str);
        message(`${this.lastKeySequence.join(" ")}:`);
        return true;
      }
    }

    return false;
  },

  isWaitingNextKey() {
    return this.lastKeySequence.length > 0;
  },

  currentKeyMapStack() {
    if(this.isMiniBuffer)
      return this.miniKeyMapStack;
    return this.keyMapStack;
  },

  handleKeyDown(str) {
    let kstack = this.currentKeyMapStack();
    for (let i = kstack.length() - 1; i >= 0; i--) {
      if (this.handleOneKeyMap(kstack.getKeyMap(i), str)) {
        return;
      }
    }

    message(`unknown key: ${str}, ${JSON.stringify(this.lastKeySequence)}`);
    this.lastKeySequence.length = 0;
  },

  pushKeyMap(keymap) {
    this.currentKeyMapStack().pushKeyMap(keymap);
  },

  popKeyMap() {
    this.currentKeyMapStack().popKeyMap();
  },

  requestDelegateKeyHandle() {
    this.delegateRequest = true;
  }
};

function CreateDefaultKeyMap() {
  let keymap = new KeyMap();
  keymap.defineDefaultSelfInsert();
  keymap.defineKey("Space", ()=> { insert(" "); })
  keymap.defineKey("Return", ()=> { insert("\n"); })
  keymap.defineKey("Backspace", delete_backward_char)
  keymap.defineKey("Delete", delete_char);
  keymap.defineKey("Left", backward_char)
  keymap.defineKey("Right", forward_char)
  keymap.defineKey("Down", next_line)
  keymap.defineKey("Up", previous_line)
  keymap.defineKey("C-b", backward_char)
  keymap.defineKey("C-f", forward_char)
  keymap.defineKey("C-n", next_line);
  keymap.defineKey("C-p", previous_line);
  keymap.defineKey("C-a", beginning_of_line)
  keymap.defineKey("C-e", end_of_line)
  keymap.defineKey("C-h", delete_backward_char)
  keymap.defineKey("C-d", delete_char);
  keymap.defineKey(["C-x", "C-s"], saveBuffer);
  keymap.defineKey(["C-x", "C-f"], find_file);
  keymap.defineKey(["C-x", "C-n"], new_file);
  keymap.defineKey("M->", end_of_buffer);
  keymap.defineKey("M-<", beginning_of_buffer);
  keymap.defineKey("C-v", next_page)
  keymap.defineKey("M-v", previous_page)
  keymap.defineKey("C-Space", set_mark_command);
  keymap.defineKey("C-@", set_mark_command);
  keymap.defineKey(["C-x", "C-x"], exchange_point_and_mark);
  keymap.defineKey("C-j", eval_region);
  keymap.defineKey("M-w", copy_region);
  keymap.defineKey("C-w", kill_region);
  keymap.defineKey("C-k", kill_line);
  keymap.defineKey("C-y", yank);
  keymap.defineKey("M-x", execute_extended_command);
  keymap.defineKey(["C-x", "2"], split_window);
  keymap.defineKey(["C-x", "0"], delete_window);
  keymap.defineKey(["C-x", "o"], other_window);
  keymap.defineKey(["C-x", "1"], delete_other_windows);
  keymap.defineKey(["C-x", "b"], switch_to_buffer);
  keymap.defineKey("C-s", isearch_forward);
  keymap.defineKey("C-r", isearch_backward);
  return keymap;
}


function CreateDefaultMiniBufferKeyMap()
{
  let keymap = CreateDefaultKeyMap();
  keymap.removeKey("Return");
  keymap.removeKey(["C-x", "C-s"]);
  keymap.removeKey(["C-x", "C-f"]);
  keymap.removeKey("C-j");
  keymap.removeKey("M-x");
  keymap.removeKey(["C-x", "2"]);
  keymap.removeKey(["C-x", "0"]);
  keymap.removeKey(["C-x", "o"]);
  keymap.removeKey(["C-x", "1"]);
  return keymap;
}

function onKeyDown(str) {
   g_keyMapHandler.handleKeyDown(str);
}

