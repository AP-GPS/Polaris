const express = require("express");
const { fetchConfig, updateConfig } = require("../controllers/configController");
const { protect, restrictTo } = require("../middleware/auth");
const router = express.Router();

/**
 * @swagger
 * /config:
 *   get:
 *     summary: Fetch current configuration
 *     tags:
 *       - Config
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Configuration retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 intervalMinutes:
 *                   type: integer
 *                 thresholdSignal:
 *                   type: integer
 *                 thresholdDownload:
 *                   type: number
 *                 thresholdUpload:
 *                   type: number
 *                 thresholdPing:
 *                   type: integer
 *       401:
 *         description: Unauthorized (missing or invalid token)
 */
router.get("/config", protect, fetchConfig);

/**
 * @swagger
 * /config:
 *   patch:
 *     summary: Update configuration (admin only)
 *     tags:
 *       - Config
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               intervalMinutes:
 *                 type: integer
 *                 description: Interval for measurements in minutes
 *               thresholdSignal:
 *                 type: integer
 *                 description: Signal strength threshold
 *               thresholdDownload:
 *                 type: number
 *                 description: Download speed threshold
 *               thresholdUpload:
 *                 type: number
 *                 description: Upload speed threshold
 *               thresholdPing:
 *                 type: integer
 *                 description: Ping time threshold
 *     responses:
 *       200:
 *         description: Configuration updated successfully
 *       400:
 *         description: Invalid request body
 *       401:
 *         description: Unauthorized (missing or invalid token)
 *       403:
 *         description: Forbidden (insufficient permissions)
 */
router.patch("/config", protect, restrictTo(["admin"]), updateConfig);

module.exports = router;
