import React, { useEffect, useState } from "react";
import {
    Container,
    Box,
    Typography,
    TextField,
    Button,
    Alert,
    Checkbox,
    FormControlLabel,
    FormGroup,
    Paper,
    Divider,
    Grid
} from "@mui/material";
import { privateApi } from "../services/api";
import { useNavigate } from "react-router-dom";

const availableTests = [
    { key: "download", label: "دانلود (Download)" },
    { key: "upload", label: "بارگذاری (Upload)" },
    { key: "ping", label: "پینگ (Response Time)" },
    { key: "dns", label: "DNS" },
    { key: "web", label: "Web" },
    { key: "sms", label: "SMS" },
];

interface Config {
    intervalMinutes: number;
    thresholdSignal: number;
    thresholdDownload: number;
    thresholdUpload: number;
    thresholdPing: number;
}

interface TestDefinition {
    id: number;
    name: string;
    tests: string[];
}

const Settings: React.FC = () => {
    const navigate = useNavigate();

    const getConfig = () =>
        privateApi.get<Config>("/config");

    const updateConfig = (config: Config) =>
        privateApi.patch("/config", config);

    const getTests = () =>
        privateApi.get<TestDefinition[]>("/tests/definitions");

    const createTest = (def: { name: string; tests: string[] }) =>
        privateApi.post<TestDefinition>("/tests/definitions", def);

    const [config, setConfig] = useState<Config>({
        intervalMinutes: 0,
        thresholdSignal: 0,
        thresholdDownload: 0,
        thresholdUpload: 0,
        thresholdPing: 0,
    });
    const [tests, setTests] = useState<TestDefinition[]>([]);
    const [newTestName, setNewTestName] = useState("");
    const [selectedTests, setSelectedTests] = useState<string[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    useEffect(() => {
        async function fetchData() {
            try {
                const cfg = await getConfig();
                setConfig(cfg.data);
                const tst = await getTests();
                setTests(tst.data);
            } catch (err: any) {
                setError(err.response?.data?.message || "خطا در بارگذاری تنظیمات");
            }
        }
        fetchData();
    }, []);

    const handleConfigChange =
        (field: keyof Config) =>
            (e: React.ChangeEvent<HTMLInputElement>) =>
                setConfig(prev => ({ ...prev, [field]: Number(e.target.value) }));

    const handleSaveConfig = async () => {
        setError(null); setSuccess(null);
        try {
            await updateConfig(config);
            setSuccess("تنظیمات با موفقیت ذخیره شد");
        } catch (err: any) {
            setError(err.response?.data?.message || "خطا در ذخیره تنظیمات");
        }
    };

    const handleTestToggle = (key: string) => (e: React.ChangeEvent<HTMLInputElement>) => {
        setSelectedTests(prev =>
            e.target.checked ? [...prev, key] : prev.filter(t => t !== key)
        );
    };

    const handleAddTest = async () => {
        if (!newTestName || selectedTests.length === 0) {
            setError("نام تست و حداقل یک آزمون باید انتخاب شود");
            return;
        }
        setError(null); setSuccess(null);
        try {
            const res = await createTest({ name: newTestName, tests: selectedTests });
            setTests(prev => [...prev, res.data]);
            setNewTestName("");
            setSelectedTests([]);
            setSuccess("تست جدید با موفقیت افزوده شد");
        } catch (err: any) {
            setError(err.response?.data?.message || "خطا در افزودن تست");
        }
    };

    return (
        <Grid width="100vw">
            <Container maxWidth="lg" sx={{ my: 2 }}>
                <Box display="flex" justifyContent="space-between" alignItems="center" pb={1}>
                    <Button variant="outlined" onClick={() => navigate("/")}>
                        داشبورد
                    </Button>
                    <Typography variant="h5">تنظیمات</Typography>
                </Box>

                {(error || success) && (
                    <Alert dir="rtl" severity={error ? "error" : "success"} sx={{ mb: 2 }}>
                        {error || success}
                    </Alert>
                )}

                <Paper sx={{ p: 2, mb: 3 }}>
                    <Typography variant="h6">پیکربندی کلی</Typography>
                    <Box sx={{ display: "flex", flexWrap: "wrap", gap: 2, mt: 2 }}>
                        <TextField
                            fullWidth
                            label="فواصل (دقیقه)"
                            type="number"
                            value={config.intervalMinutes}
                            onChange={handleConfigChange("intervalMinutes")}
                        />
                        <TextField
                            fullWidth
                            label="آستانه سیگنال"
                            type="number"
                            value={config.thresholdSignal}
                            onChange={handleConfigChange("thresholdSignal")}
                        />
                        <TextField
                            fullWidth
                            label="آستانه دانلود"
                            type="number"
                            value={config.thresholdDownload}
                            onChange={handleConfigChange("thresholdDownload")}
                        />
                        <TextField
                            fullWidth
                            label="آستانه آپلود"
                            type="number"
                            value={config.thresholdUpload}
                            onChange={handleConfigChange("thresholdUpload")}
                        />
                        <TextField
                            fullWidth
                            label="آستانه پینگ"
                            type="number"
                            value={config.thresholdPing}
                            onChange={handleConfigChange("thresholdPing")}
                        />
                    </Box>
                    <Button fullWidth variant="contained" sx={{ mt: 2 }} onClick={handleSaveConfig}>
                        ذخیره پیکربندی
                    </Button>
                </Paper>

                <Paper sx={{ p: 2 }}>
                    <Typography variant="h6">تعریف تست جدید</Typography>
                    <Box sx={{ mt: 2 }}>
                        <TextField
                            label="نام تست"
                            fullWidth
                            value={newTestName}
                            onChange={e => setNewTestName(e.target.value)}
                        />
                        <FormGroup row sx={{ mt: 2 }}>
                            {availableTests.map(t => (
                                <FormControlLabel
                                    key={t.key}
                                    control={
                                        <Checkbox
                                            checked={selectedTests.includes(t.key)}
                                            onChange={handleTestToggle(t.key)}
                                        />
                                    }
                                    label={t.label}
                                />
                            ))}
                        </FormGroup>
                        <Button fullWidth variant="contained" sx={{ mt: 2 }} onClick={handleAddTest}>
                            افزودن تست
                        </Button>
                    </Box>

                    <Divider sx={{ my: 3 }} />

                    <Typography variant="subtitle1">لیست تعریف‌شده تست‌ها</Typography>
                    <Box component="ul">
                        {tests.map(t => (
                            <li key={t.id}>
                                <strong>{t.name}</strong>: {t.tests.join(", ")}
                            </li>
                        ))}
                    </Box>
                </Paper>
            </Container>
        </Grid>
    );
};

export default Settings;
