const fs = require("fs");
const path = require("path");

function pingTest(req, res) {
  res.sendStatus(200);
}

function downloadTest(req, res) {
  const filePath = path.join(__dirname, "../utils/testfile.bin");
  const stat = fs.statSync(filePath);
  res.writeHead(200, {
    "Content-Type": "application/octet-stream",
    "Content-Length": stat.size
  });
  const stream = fs.createReadStream(filePath);
  stream.pipe(res);
}

function uploadTest(req, res) {
  res.sendStatus(200);
}

module.exports = { pingTest, downloadTest, uploadTest };
