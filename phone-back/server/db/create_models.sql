CREATE DATABASE IF NOT EXISTS polaris_db;
USE polaris_db;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role ENUM('user','admin') NOT NULL DEFAULT 'user'
);

CREATE TABLE IF NOT EXISTS measurements (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  userId BIGINT NOT NULL,
  timestamp BIGINT NOT NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  cellId INT NOT NULL,
  lac INT NOT NULL,
  mcc INT NOT NULL,
  mnc INT NOT NULL,
  signalStrength INT NOT NULL,
  networkType VARCHAR(20) NOT NULL,
  downloadSpeed DOUBLE NOT NULL,
  uploadSpeed DOUBLE NOT NULL,
  pingTime BIGINT NOT NULL,
  FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS config (
  id INT PRIMARY KEY,
  intervalMinutes INT NOT NULL,
  thresholdSignal INT NOT NULL,
  thresholdDownload DOUBLE NOT NULL,
  thresholdUpload DOUBLE NOT NULL,
  thresholdPing BIGINT NOT NULL
);

INSERT INTO config (id, intervalMinutes, thresholdSignal, thresholdDownload, thresholdUpload, thresholdPing)
VALUES (1, 15, -9999, -9999, -9999, 999999999)
ON DUPLICATE KEY UPDATE
  intervalMinutes=VALUES(intervalMinutes),
  thresholdSignal=VALUES(thresholdSignal),
  thresholdDownload=VALUES(thresholdDownload),
  thresholdUpload=VALUES(thresholdUpload),
  thresholdPing=VALUES(thresholdPing);
