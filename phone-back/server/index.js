const express = require("express");
const cors = require("cors");
const dotenv = require("dotenv");
dotenv.config();

const initDB = require("./db/index");
const swaggerUi = require("swagger-ui-express");
const swaggerSpec = require("./swagger");
const authRoutes = require("./routes/authRoutes");
const measurementRoutes = require("./routes/measurementRoutes");
const configRoutes = require("./routes/configRoutes");
const testRoutes = require("./routes/testRoutes");

const app = express();
app.use(cors());
app.use(express.json());
app.use("/api", swaggerUi.serve, swaggerUi.setup(swaggerSpec));

initDB
  .then((pool) => {
    app.locals.db = pool;

    app.use("/auth", authRoutes);
    app.use("/", measurementRoutes);
    app.use("/", configRoutes);
    app.use("/test", testRoutes);

    const PORT = process.env.PORT || 3000;
    app.listen(PORT, () => {
      console.log(`Server listening on port ${PORT}`);
      console.log(`Swagger docs available at http://localhost:${PORT}/api`);
    });
  })
  .catch((err) => {
    console.error("Failed to initialize database connection:", err);
    process.exit(1);
  });
