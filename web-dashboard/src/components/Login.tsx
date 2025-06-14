import React, { useEffect, useState } from "react";
import { Box, Button, Container, TextField, Typography, Alert, Grid } from "@mui/material";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { publicApi } from "../services/api"
import { Link as RouterLink } from "react-router-dom";
import { Link } from "@mui/material";

const Login: React.FC = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const response = await publicApi.post("/auth/login", { username, password });
      const { token } = response.data as { token: string };
      localStorage.setItem("jwt_token", token);
      login(token);
      navigate("/", { replace: true });
    } catch (err: any) {
      setError(err.response?.data?.message || "خطا در ارتباط با سرور");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Grid width={'100vw'}>
      <Container maxWidth="xs">
        <Box mt={8} display="flex" flexDirection="column" alignItems="center">
          <Typography component="h1" variant="h5">ورود</Typography>
          {error && <Alert severity="error" sx={{ width: "100%", mt: 2 }}>{error}</Alert>}
          <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2, width: "100%" }}>
            <TextField
              label="نام کاربری"
              fullWidth
              margin="normal"
              value={username}
              onChange={e => setUsername(e.target.value)}
            />
            <TextField
              label="رمز عبور"
              type="password"
              fullWidth
              margin="normal"
              value={password}
              onChange={e => setPassword(e.target.value)}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              disabled={loading}
              sx={{ mt: 2, direction: 'rtl' }}
            >
              {loading ? "در حال بررسی ..." : "ورود"}
            </Button>
          </Box>
        </Box>
        <Box mt={2} textAlign="center">
          <Link component={RouterLink} to="/register">ثبت نام</Link>
        </Box>
      </Container>
    </Grid>
  );
};

export default Login;
