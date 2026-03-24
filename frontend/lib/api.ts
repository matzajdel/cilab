import axios from 'axios';
import { API_BASE_URL } from './config';

const api = axios.create({
    baseURL: `${API_BASE_URL}`,
    headers: {
        'Content-Type': 'application/json',
    },
});

const KEYCLOAK_TOKEN_URL =
    process.env.NEXT_PUBLIC_KEYCLOAK_TOKEN_URL ||
    'http://localhost:8085/realms/cilab-realm/protocol/openid-connect/token';

type RefreshResponse = {
    accessToken: string;
    refreshToken: string;
};

let refreshPromise: Promise<RefreshResponse> | null = null;

const clearAuthAndRedirect = () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    window.location.href = '/sign-in';
};

const requestTokenRefresh = async (): Promise<RefreshResponse> => {
    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
        throw new Error('No refresh token');
    }

    const params = new URLSearchParams();
    params.append('client_id', 'cilab-client');
    params.append('grant_type', 'refresh_token');
    params.append('refresh_token', refreshToken);

    const tokenResponse = await axios.post(KEYCLOAK_TOKEN_URL, params, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    });

    return {
        accessToken: tokenResponse.data.access_token,
        refreshToken: tokenResponse.data.refresh_token,
    };
};

api.interceptors.request.use(
    (config) => {
        if (typeof window !== 'undefined') {
            const token = localStorage.getItem('access_token');
            
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

api.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {

        if (typeof window === 'undefined') {
            return Promise.reject(error);
        }

        const originalRequest = error.config;

        if (!originalRequest) {
            return Promise.reject(error);
        }

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                if (!refreshPromise) {
                    refreshPromise = requestTokenRefresh().finally(() => {
                        refreshPromise = null;
                    });
                }

                const { accessToken: newAccessToken, refreshToken: newRefreshToken } = await refreshPromise;

                localStorage.setItem('access_token', newAccessToken);
                localStorage.setItem('refresh_token', newRefreshToken);

                originalRequest.headers = originalRequest.headers || {};
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                
                return api(originalRequest);

            } catch (refreshError) {
                console.error("Session expired. Logging out...", refreshError);
                clearAuthAndRedirect();
                
                return Promise.reject(refreshError);
            }
        }
        
        return Promise.reject(error);
    }
);

export default api;