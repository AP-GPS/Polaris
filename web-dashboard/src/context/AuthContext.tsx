import React, {
  createContext,
  useState,
  useEffect,
  useContext,
} from "react";
import type { ReactNode } from "react";

interface AuthContextType {
  token: string | null;
  login: (jwt: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType>({
  token: null,
  login: () => {},
  logout: () => {},
});

export const useAuth = () => useContext(AuthContext);

interface Props {
  children: ReactNode;
}

export const AuthProvider: React.FC<Props> = ({ children }) => {
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    const stored = localStorage.getItem("jwt_token");
    if (stored) {
      setToken(stored);
    }
  }, []);

  const login = (jwt: string) => {
    localStorage.setItem("jwt_token", jwt);
    setToken(jwt);
  };

  const logout = () => {
    localStorage.removeItem("jwt_token");
    setToken(null);
  };

  return (
    <AuthContext.Provider value={{ token, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
