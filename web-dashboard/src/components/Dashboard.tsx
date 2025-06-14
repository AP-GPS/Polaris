import React, { useEffect, useState } from "react";
import {
  Container,
  Typography,
  Box,
  Button,
  Alert,
  Grid,
} from "@mui/material";
import { DataGrid } from "@mui/x-data-grid";
import type { GridColDef, GridCellParams } from "@mui/x-data-grid";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { privateApi } from "../services/api";

export interface Measurement {
  id: number;
  userId: number;
  timestamp: number;
  n: number;
  s: string;
  t: string;
  band: string;
  ulArfcn: number;
  dlArfcn: number;
  code: number;
  ulBw: number;
  dlBw: number;
  plmnId: number;
  tacOrLac: number;
  rac: number;
  longCellId: number;
  siteId: number;
  cellId: number;
  latitude: number;
  longitude: number;
  signalStrength: number;
  networkType: string;
  downloadSpeed: number;
  uploadSpeed: number;
  pingTime: number;
}

interface Config {
  intervalMinutes: number;
  thresholdSignal: number;
  thresholdDownload: number;
  thresholdUpload: number;
  thresholdPing: number;
}

const Dashboard: React.FC = () => {
  const [data, setData] = useState<Measurement[]>([]);
  const [config, setConfig] = useState<Config | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { logout } = useAuth();
  const navigate = useNavigate();

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await privateApi.get<Measurement[]>("/measurements");
      setData(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message || "خطا در واکشی داده‌ها");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    (async () => {
      try {
        const res = await privateApi.get<Config>("/config");
        setConfig(res.data);
      } catch (err) {
        console.error("خطا در بارگذاری تنظیمات:", err);
      }
    })();
  }, []);

  useEffect(() => {
    if (!config) return;
    const id = setInterval(fetchData, config.intervalMinutes * 60_000);
    return () => clearInterval(id);
  }, [config]);

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  // ستون‌های جدول
  const columns: GridColDef[] = [
    { field: "id", headerName: "ID", width: 60 },
    {
      field: "timestamp",
      headerName: "Timestamp",
      width: 180,
      valueFormatter: ({ value }) =>
        new Date(value as number).toLocaleString(),
    },
    { field: "n", headerName: "N", width: 80 },
    { field: "s", headerName: "S", width: 80 },
    { field: "t", headerName: "T", width: 80 },
    { field: "band", headerName: "Band", width: 100 },
    { field: "ulArfcn", headerName: "UL ARFCN", width: 120 },
    { field: "dlArfcn", headerName: "DL ARFCN", width: 120 },
    { field: "code", headerName: "Code", width: 80 },
    { field: "ulBw", headerName: "UL BW", width: 100 },
    { field: "dlBw", headerName: "DL BW", width: 100 },
    { field: "plmnId", headerName: "PLMN ID", width: 100 },
    { field: "tacOrLac", headerName: "TAC/LAC", width: 100 },
    { field: "rac", headerName: "RAC", width: 80 },
    { field: "longCellId", headerName: "Long Cell ID", width: 140 },
    { field: "siteId", headerName: "Site ID", width: 100 },
    { field: "cellId", headerName: "Cell ID", width: 100 },
    { field: "latitude", headerName: "Latitude", width: 140 },
    { field: "longitude", headerName: "Longitude", width: 140 },
    {
      field: "signalStrength",
      headerName: "Signal Strength",
      width: 140,
    },
    { field: "networkType", headerName: "Network Type", width: 120 },
    {
      field: "downloadSpeed",
      headerName: "Download (Mbps)",
      width: 150,
    },
    { field: "uploadSpeed", headerName: "Upload (Mbps)", width: 150 },
    { field: "pingTime", headerName: "Ping (ms)", width: 120 },
  ];

  const exportToCSV = () => {
    const header = columns.map(col => `"${col.headerName}"`).join(",");
    const rows = data.map(row =>
      columns
        .map(col => {
          let val: any = (row as any)[col.field];
          if (col.field === "timestamp") {
            val = new Date(val as number).toLocaleString();
          }
          return `"${val}"`;
        })
        .join(",")
    );
    const csv = [header, ...rows].join("\r\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.setAttribute("download", "polaris_data.csv");
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  const exportToKML = () => {
    const kmlHeader =
      '<?xml version="1.0" encoding="UTF-8"?>\n' +
      '<kml xmlns="http://www.opengis.net/kml/2.2">\n<Document>\n';
    const placemarks = data
      .map(row => {
        const time = new Date(row.timestamp).toLocaleString();
        return `<Placemark>
  <name>${row.id}</name>
  <description><![CDATA[
    Timestamp: ${time}<br/>
    Download: ${row.downloadSpeed} Mbps<br/>
    Upload: ${row.uploadSpeed} Mbps<br/>
    Ping: ${row.pingTime} ms<br/>
    Network: ${row.networkType}
  ]]></description>
  <Point><coordinates>${row.longitude},${row.latitude},0</coordinates></Point>
</Placemark>`;
      })
      .join("\n");
    const kmlFooter = "\n</Document>\n</kml>";
    const kml = kmlHeader + placemarks + kmlFooter;
    const blob = new Blob([kml], {
      type: "application/vnd.google-earth.kml+xml",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.setAttribute("download", "polaris_data.kml");
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  return (
    <Grid width="100vw">
      <Typography sx={{ textAlign: 'center', display: { xs: 'block', md: 'none' } }} variant="h5">داشبورد پولاریس</Typography>
      <Container maxWidth="lg" sx={{ my: 2 }}>
        <Box
          display="flex"
          flexDirection="row"
          justifyContent="space-between"
          alignItems="flex-start"
          pb={1}
        >
          <Box display='flex' gap={1} flexWrap='wrap' pr={1}>
            <Button variant="outlined" color="error" onClick={handleLogout}>
              خروج
            </Button>
            <Button
              variant="outlined"
              onClick={() => navigate("/settings")}
              color="info"
            >
              تنظیمات
            </Button>
            <Button
              variant="outlined"
              onClick={() => navigate('/charts')}
              color="secondary"
            >
              نمودارها
            </Button>
            <Button
              variant="outlined"
              onClick={() => navigate('/map')}
              color="inherit"
            >
              نقشه
            </Button>
            <Button
              variant="outlined"
              onClick={exportToCSV}
              color="success"
            >
              خروجی CSV
            </Button>
            <Button
              variant="outlined"
              onClick={exportToKML}
              color="warning"
            >
              خروجی KML
            </Button>
          </Box>
          <Typography sx={{ direction: 'rtl', display: {xs: 'none', md: 'block'} }} variant="h5">داشبورد پولاریس</Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Box sx={{ height: 600, width: "100%" }}>
          <DataGrid
            rows={data}
            columns={columns}
            loading={loading}
            getRowId={row => row.id}
            initialState={{
              pagination: {
                paginationModel: { pageSize: 10, page: 0 },
              },
            }}
            pageSizeOptions={[10, 25, 50]}
            getCellClassName={(params: GridCellParams) => {
              if (!config) return "";
              const { field, value } = params;
              if (typeof value !== "number") return "";
              switch (field) {
                case "downloadSpeed":
                  return value < config.thresholdDownload
                    ? "low-download"
                    : "";
                case "uploadSpeed":
                  return value < config.thresholdUpload
                    ? "low-upload"
                    : "";
                case "pingTime":
                  return value > config.thresholdPing
                    ? "high-ping"
                    : "";
                case "signalStrength":
                  return value < config.thresholdSignal
                    ? "weak-signal"
                    : "";
                default:
                  return "";
              }
            }}
            sx={{
              "& .MuiDataGrid-row:nth-of-type(odd)": {
                bgcolor: theme => theme.palette.action.hover,
              },
              "& .MuiDataGrid-row:nth-of-type(even)": {
                bgcolor: "white",
              },
              "& .low-download": {
                bgcolor: theme => theme.palette.warning.light,
              },
              "& .low-upload": {
                bgcolor: theme => theme.palette.warning.light,
              },
              "& .high-ping": {
                bgcolor: theme => theme.palette.error.light,
              },
              "& .weak-signal": {
                bgcolor: theme => theme.palette.info.light,
              },
            }}
          />
        </Box>
      </Container>
    </Grid>
  );
};

export default Dashboard;
