import React from 'react';
import {useNavigate} from "react-router-dom";

import RecipeForm from "../RecipeForm/RecipeForm";
import {createRecipe} from "../../apis/recipeApi";

const EMPTY_RECIPE = {
    name: '',
    description: '',
    imageUrl: '',
    ingredients: []
}

function AddRecipe() {
    const navigate = useNavigate()

    return (
        <RecipeForm
            title="Add Recipe"
            submitLabel="Submit"
            initialRecipe={EMPTY_RECIPE}
            onSubmit={(recipe) => createRecipe(recipe).then(() => navigate("/"))}
        />
    )
}

export default AddRecipe;
