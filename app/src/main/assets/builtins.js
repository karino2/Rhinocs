function self_insert() {
   insert($key);
}

function delete_backward_char(n=1) {
   let end = point();
   backward_char(n);
   delete_region(point(), end);
}

function find_file() {
   let [uri, fname] = select_file("*/*");
   print(`deb: ${uri}, ${fname}`);
   open_uri(uri);
}

function saveBuffer() {
   save_buffer();
   show_toast("Saved!");
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
  keymap.defineKey(["C-x", "C-s"], saveBuffer);
  keymap.defineKey(["C-x", "C-f"], find_file);
  keymap.defineKey("M->", end_of_buffer);
  keymap.defineKey("M-<", beginning_of_buffer);
  keymap.defineKey("C-v", next_page)
  keymap.defineKey("M-v", previous_page)
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
        print("waiting next key");
        this.lastKeySequence.push(str);
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

    print(`unknown key: ${str}, ${JSON.stringify(this.lastKeySequence)}`);
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

function global_set_key(keyPat, func) {
    g_keyMapHandler.currentKeyMap().defineKey(keyPat, func);
}


function defaultOnKeyDown(str) {
   g_keyMapHandler.handleKeyDown(str);
}

function onKeyDown(str) {
   g_keyMapHandler.handleKeyDown(str);
}
