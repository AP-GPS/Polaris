import React, { useState } from "react";
import { Container, Box, TextField, Button, Typography, Alert, Link, Grid } from "@mui/material";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { publicApi } from "../services/api";

const Register: React.FC = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            await publicApi.post("/auth/register", { username, password });
            navigate("/login");
        } catch (err: any) {
            setError(err.response?.data?.message || "خطا در ثبت نام");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Grid width={'100vw'}>
            <Container maxWidth="xs">
                <Box mt={8} display="flex" flexDirection="column" alignItems="center">
                    <Typography variant="h5">ثبت نام</Typography>
                    {error && <Alert severity="error" sx={{ width: "100%", mt: 2 }}>{error}</Alert>}
                    <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2, width: "100%" }}>
                        <TextField label="نام کاربری" fullWidth margin="normal" value={username} onChange={e => setUsername(e.target.value)} />
                        <TextField label="رمز عبور" type="password" fullWidth margin="normal" value={password} onChange={e => setPassword(e.target.value)} />
                        <Button type="submit" fullWidth variant="contained" disabled={loading} sx={{ mt: 2 }}>
                            {loading ? "در حال ثبت..." : "ثبت نام"}
                        </Button>
                        <Box mt={2} textAlign="center">
                            <Link component={RouterLink} to="/login">حساب دارید؟ ورود</Link>
                        </Box>
                    </Box>
                </Box>
            </Container>
        </Grid>
    );
};

export default Register;
