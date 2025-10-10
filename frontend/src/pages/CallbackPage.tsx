// // src/pages/CallbackPage.tsx
// import { useEffect } from "react";
// import { userManager } from "@/config/oidc.config";
// import { useNavigate } from "react-router-dom";

// const CallbackPage = () => {

//     const navigate = useNavigate();

//   // src/pages/CallbackPage.tsx
// useEffect(() => {
//   const handleCallback = async () => {
//     try {
//       console.log('Callback başladı');
//       console.log('URL:', window.location.href);
//       console.log('Cookies:', document.cookie);
      
//       const user = await userManager.signinRedirectCallback();
//       console.log('Login successsfull:', user);
//       navigate('/');
//     } catch (error) {
//       console.error('OIDC callback error:', error);
//       console.log('Tüm cookies:', document.cookie);
//     }
//   };

//   handleCallback();
// }, [navigate]);

//   return <div>Loading...</div>;
// };

// export default CallbackPage;
