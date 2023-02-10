importScripts("../js/lib/vyxal.js")
self.addEventListener('message', function (e) {
    var data = e.data;
    console.log("Worker received: " + data.mode);
    var sendFn = x => {
        this.postMessage({ "val": x, "command": "append" })
    }

    Vyxal.execute(data.code, data.inputs, data.flags, sendFn.toString())
    this.postMessage({ "command": "done" })
})
