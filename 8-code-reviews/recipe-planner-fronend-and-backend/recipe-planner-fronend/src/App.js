import MyNavbar from "./components/MyNavbar/MyNavbar";
import Browse from "./components/Browse/Browse";
import {Route, Routes} from "react-router-dom";
import Planer from "./components/Planer/Planer";
import AddRecipe from "./components/AddRecipe/AddRecipe";


function App() {
  return (
      <>


      <MyNavbar/>
      <div>
        <Routes>
          <Route path="/" element={<Browse/>} />
          <Route path="/planer" element={<Planer/>} />
          <Route path="/new-menues" element={<AddRecipe/>} />
          <Route path="/recipes/:recipeId/edit" element={<AddRecipe/>} />
        </Routes>
      </div>
      </>
  );
}

export default App;
