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

let lastKeySequence = []

function defineKey(kmap, keySeq, func) {
  if (typeof keySeq === 'string') {
    kmap[keySeq] = func;
    return;
  }

  let current = kmap;
  for(let i = 0; i < keySeq.length-1; i++) {
    let k = keySeq[i];
    let v = current[k];
    if (!v || typeof v !== 'object') current[k] = {}

    current = current[k];
  }
  current[keySeq[keySeq.length-1]] = func
}

function lookupLastMap(kmap, keySeq) {
  if(keySeq.length == 0)
    return kmap;

  let current = kmap;
  for (let i = 0; i < keySeq.length; i++) {
    if (!current || typeof current !== 'object') return undefined;
    current = current[keySeq[i]];
  }
  return current;
}

let defSelfKeys = default_self_insert_keys();
let keyMap = {};
defSelfKeys.forEach(k => keyMap[k] = self_insert);

keyMap["Space"] = ()=> { insert(" "); }
keyMap["Return"] = ()=> { insert("\n"); }
keyMap["Backspace"] = delete_backward_char;

defineKey(keyMap, "Left", backward_char)
defineKey(keyMap, "Right", forward_char)
defineKey(keyMap, "Down", next_line)
defineKey(keyMap, "Up", previous_line)
defineKey(keyMap, "C-b", backward_char)
defineKey(keyMap, "C-f", forward_char)
defineKey(keyMap, "C-n", next_line);
defineKey(keyMap, "C-p", previous_line);
defineKey(keyMap, ["C-x", "C-s"], saveBuffer);
defineKey(keyMap, ["C-x", "C-f"], find_file);
defineKey(keyMap, "M->", end_of_buffer);
defineKey(keyMap, "M-<", beginning_of_buffer);

function defaultOnKeyDown(str) {
    print(`deb: ${JSON.stringify(lastKeySequence)}, ${str}`);

    let lmap = lookupLastMap(keyMap, lastKeySequence);
    if (lmap) {
       let v = lmap[str];
       if (typeof v === 'function') {
          lastKeySequence.length = 0;
          return v();
       }
       if (v !== null && typeof v === 'object') {
          print("waiting next key");
          lastKeySequence.push(str);
          return;
       }
    }

    print(`unknown key: ${str}, ${JSON.stringify(lastKeySequence)}`);
    lastKeySequence.length = 0;
}

function onKeyDown(str) {
   defaultOnKeyDown(str);
}
