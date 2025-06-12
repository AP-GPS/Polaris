-- CreateTable
CREATE TABLE `User` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(191) NOT NULL,
    `password` VARCHAR(191) NOT NULL,
    `role` ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',

    UNIQUE INDEX `User_username_key`(`username`),
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `Config` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `intervalMinutes` INTEGER NOT NULL,
    `thresholdSignal` INTEGER NOT NULL,
    `thresholdDownload` INTEGER NOT NULL,
    `thresholdUpload` INTEGER NOT NULL,
    `thresholdPing` INTEGER NOT NULL,

    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `Measurement` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `userId` INTEGER NOT NULL,
    `timestamp` BIGINT NOT NULL,
    `n` INTEGER NOT NULL,
    `s` INTEGER NOT NULL,
    `t` INTEGER NOT NULL,
    `band` INTEGER NOT NULL,
    `ulArfcn` INTEGER NOT NULL,
    `dlArfcn` INTEGER NOT NULL,
    `code` INTEGER NOT NULL,
    `ulBw` DOUBLE NOT NULL,
    `dlBw` DOUBLE NOT NULL,
    `plmnId` INTEGER NOT NULL,
    `tacOrLac` INTEGER NOT NULL,
    `rac` INTEGER NOT NULL,
    `longCellId` INTEGER NOT NULL,
    `siteId` INTEGER NOT NULL,
    `cellId` INTEGER NOT NULL,
    `latitude` DOUBLE NOT NULL,
    `longitude` DOUBLE NOT NULL,
    `signalStrength` INTEGER NOT NULL,
    `networkType` VARCHAR(191) NOT NULL,
    `downloadSpeed` DOUBLE NOT NULL,
    `uploadSpeed` DOUBLE NOT NULL,
    `pingTime` INTEGER NOT NULL,

    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `TestDefinition` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(191) NOT NULL,
    `tests` JSON NOT NULL,
    `userId` INTEGER NOT NULL,

    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AddForeignKey
ALTER TABLE `Measurement` ADD CONSTRAINT `Measurement_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `User`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `TestDefinition` ADD CONSTRAINT `TestDefinition_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `User`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;
