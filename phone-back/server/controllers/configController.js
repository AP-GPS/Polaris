const pool = require("../db/index");

async function fetchConfig(req, res) {
  try {
    const [rows] = await (await pool).execute("SELECT * FROM config WHERE id = 1");
    if (!rows.length) {
      return res.status(404).json({ message: "Config not found" });
    }
    const cfg = rows[0];
    res.json({
      intervalMinutes: cfg.intervalMinutes,
      thresholdSignal: cfg.thresholdSignal,
      thresholdDownload: cfg.thresholdDownload,
      thresholdUpload: cfg.thresholdUpload,
      thresholdPing: cfg.thresholdPing
    });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
}

async function updateConfig(req, res) {
  try {
    const { intervalMinutes, thresholdSignal, thresholdDownload, thresholdUpload, thresholdPing } = req.body;
    const sql = `
      UPDATE config
      SET intervalMinutes = ?, thresholdSignal = ?, thresholdDownload = ?, thresholdUpload = ?, thresholdPing = ?
      WHERE id = 1
    `;
    const params = [intervalMinutes, thresholdSignal, thresholdDownload, thresholdUpload, thresholdPing];
    await (await pool).execute(sql, params);
    res.json({ message: "Config updated" });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
}

module.exports = { fetchConfig, updateConfig };
