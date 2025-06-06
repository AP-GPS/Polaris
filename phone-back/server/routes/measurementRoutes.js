const express = require("express");
const { addMeasurement, getMeasurements } = require("../controllers/measurementController");
const { protect } = require("../middleware/auth");
const router = express.Router();

/**
 * @swagger
 * /measurements:
 *   post:
 *     summary: Add a new measurement record
 *     tags:
 *       - Measurements
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               timestamp:
 *                 type: integer
 *                 description: Unix timestamp of measurement
 *               latitude:
 *                 type: number
 *               longitude:
 *                 type: number
 *               cellId:
 *                 type: integer
 *               lac:
 *                 type: integer
 *               mcc:
 *                 type: integer
 *               mnc:
 *                 type: integer
 *               signalStrength:
 *                 type: integer
 *               networkType:
 *                 type: string
 *               downloadSpeed:
 *                 type: number
 *               uploadSpeed:
 *                 type: number
 *               pingTime:
 *                 type: integer
 *     responses:
 *       201:
 *         description: Measurement added successfully
 *       400:
 *         description: Invalid request body
 *       401:
 *         description: Unauthorized (missing or invalid token)
 */
router.post("/measurements", protect, addMeasurement);

/**
 * @swagger
 * /measurements:
 *   get:
 *     summary: Get all measurements for the authenticated user
 *     tags:
 *       - Measurements
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: List of measurements
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   id:
 *                     type: integer
 *                   timestamp:
 *                     type: integer
 *                   latitude:
 *                     type: number
 *                   longitude:
 *                     type: number
 *                   cellId:
 *                     type: integer
 *                   lac:
 *                     type: integer
 *                   mcc:
 *                     type: integer
 *                   mnc:
 *                     type: integer
 *                   signalStrength:
 *                     type: integer
 *                   networkType:
 *                     type: string
 *                   downloadSpeed:
 *                     type: number
 *                   uploadSpeed:
 *                     type: number
 *                   pingTime:
 *                     type: integer
 *       401:
 *         description: Unauthorized (missing or invalid token)
 */
router.get("/measurements", protect, getMeasurements);

module.exports = router;
