const express = require("express");
const { pingTest, downloadTest, uploadTest } = require("../controllers/testController");
const router = express.Router();

/**
 * @swagger
 * /test/ping:
 *   get:
 *     summary: Perform a ping test
 *     tags:
 *       - Tests
 *     responses:
 *       200:
 *         description: Ping test result
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 pingTime:
 *                   type: integer
 *                   description: Round-trip time in milliseconds
 */
router.get("/ping", pingTest);

/**
 * @swagger
 * /test/download:
 *   get:
 *     summary: Perform a download speed test (returns a binary file)
 *     tags:
 *       - Tests
 *     responses:
 *       200:
 *         description: Binary file for download test
 *         content:
 *           application/octet-stream:
 *             schema:
 *               type: string
 *               format: binary
 */
router.get("/download", downloadTest);

/**
 * @swagger
 * /test/upload:
 *   post:
 *     summary: Perform an upload speed test (client uploads a file)
 *     tags:
 *       - Tests
 *     requestBody:
 *       required: true
 *       content:
 *         multipart/form-data:
 *           schema:
 *             type: object
 *             properties:
 *               file:
 *                 type: string
 *                 format: binary
 *                 description: File to upload for speed test
 *     responses:
 *       200:
 *         description: Upload test succeeded
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 uploadTime:
 *                   type: integer
 *                   description: Time taken to upload in milliseconds
 *       400:
 *         description: Bad request (no file received)
 */
router.post("/upload", uploadTest);

module.exports = router;
