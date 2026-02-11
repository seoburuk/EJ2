import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import axios from 'axios';

axios.defaults.baseURL = process.env.REACT_APP_API_URL || '';
axios.defaults.withCredentials = true;

axios.interceptors.response.use(
    (response) => {
      console.log("인터셉터 확인");
      return response;
    },
    (error) => {
        // 401에러일때 작동
        if (error.response && error.response.status === 401) {
          console.log("인터셉터 확인2");
          alert("ログインが必要です。");
            
          // 로컬스토리지 정보 삭제
          localStorage.removeItem('user'); 

          window.location.href = '/login'; 
          return new Promise(() => {});
        }

        // 그 외 에러는 그대로 반환
        return Promise.reject(error);
    }
);

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
