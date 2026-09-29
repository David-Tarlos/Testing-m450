import MyNavbar from "./components/MyNavbar/MyNavbar";
import Browse from "./components/Browse/Browse";
import {Route, Routes} from "react-router-dom";
import Planer from "./components/Planer/Planer";
import AddRecipe from "./components/AddRecipe/AddRecipe";
import EditRecipe from "./components/EditRecipe/EditRecipe";


function App() {
  return (
      <>
      <MyNavbar/>
      <div>
        <Routes>
          <Route path="/" element={<Browse/>} />
          <Route path="/planer" element={<Planer/>} />
          <Route path="/new-menues" element={<AddRecipe/>} />
          <Route path="/recipes/:recipeId/edit" element={<EditRecipe/>} />
        </Routes>
      </div>
      </>
  );
}

export default App;
