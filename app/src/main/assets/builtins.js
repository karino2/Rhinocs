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

function select_file(mimeTypes) {
  return new Promise((resolve, reject)=> {
    select_file_callback(mimeTypes, resolve, reject);
  });
}

function find_file() {
  select_file(["*/*"]).then((uri, fname)=> {
    print(`deb: ${uri}, ${fname}`);
    open_uri(uri);
  })
}

function saveBuffer() {
   save_buffer();
   message("Saved!");
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
    eval(text);
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

/*
  KeyMap関連。ひとまずここに置く。
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
  return keymap;
}


let g_defaultKeyMap = CreateDefaultKeyMap();

let g_keyMapHandler = {
  lastKeySequence: [],
  delegateRequest: false,
  keyMapStack: [g_defaultKeyMap],

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

  handleKeyDown(str) {
    for (let i = this.keyMapStack.length - 1; i >= 0; i--) {
      if (this.handleOneKeyMap(this.keyMapStack[i], str)) {
        return;
      }
    }

    message(`unknown key: ${str}, ${JSON.stringify(this.lastKeySequence)}`);
    this.lastKeySequence.length = 0;
  },

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

  requestDelegateKeyHandle() {
    this.delegateRequest = true;
  }
};

/*
  g_keyMapHandlerが必要なAPI
*/

function global_set_key(keyPat, func) {
    g_keyMapHandler.currentKeyMap().defineKey(keyPat, func);
}

function read_string(prompt) {
  let miniKeyMap = new KeyMap();
  let promise = new Promise((resolve, reject)=> {
    miniKeyMap.defineKey("Return", ()=> {
      g_keyMapHandler.popKeyMap();
      let ret = leave_minibuffer()
      resolve(ret);
    });
    miniKeyMap.defineKey("C-g", ()=> {
      g_keyMapHandler.popKeyMap();
      leave_minibuffer();
      reject();
    });
  });
  g_keyMapHandler.pushKeyMap(miniKeyMap);
  enter_minibuffer(prompt);
  return promise;
}

function execute_extended_command() {
  read_string("M-x ").then((cmd)=> {
    request_function_execute(global[cmd]);
  });
}

function defaultOnKeyDown(str) {
   g_keyMapHandler.handleKeyDown(str);
}

function onKeyDown(str) {
   g_keyMapHandler.handleKeyDown(str);
}

