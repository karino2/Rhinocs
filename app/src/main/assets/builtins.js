function self_insert() {
   insert($key);
}

function delete_backward_char(n=1) {
   let end = point();
   backward_char(n);
   delete_region(point(), end);
}

function find_file() {
   let uri = select_file("*/*");
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


let defaultKeyMap = CreateDefaultKeyMap();

function KeyMapHandler() {
  this.lastKeySequence = [];
  this.keyMapStack = [defaultKeyMap];
}


KeyMapHandler.prototype.handleKeyDown = function(str) {
    let lmap = this.keyMapStack[this.keyMapStack.length-1].lookupLastMap(this.lastKeySequence);
    if (lmap) {
       let v = lmap[str];
       if (typeof v === 'function') {
          this.lastKeySequence.length = 0;
          return v();
       }
       if (v !== null && typeof v === 'object') {
          print("waiting next key");
          this.lastKeySequence.push(str);
          return;
       }
    }

    print(`unknown key: ${str}, ${JSON.stringify(this.lastKeySequence)}`);
    this.lastKeySequence.length = 0;
}

let keyMapHandler = new KeyMapHandler();

function defaultOnKeyDown(str) {
   keyMapHandler.handleKeyDown(str);
}

function onKeyDown(str) {
   keyMapHandler.handleKeyDown(str);
}
