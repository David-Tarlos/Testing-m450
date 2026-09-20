import React, {useEffect, useState} from 'react';
import './Browse.css';
import {Col, Row} from "react-bootstrap";

import Recipe from "../Recipe/Recipe";
import {fetchRecipes} from "../../apis/recipeApi";

const Browse = () => {

    const [recipes, setRecipes] = useState([]);

    useEffect(() => {
        fetchRecipes().then(setRecipes);
    }, []);

    return (
        <Row>
            {recipes.map((recipe) => (
                <Col key={recipe.id} sm={12} md={6} lg={4} xl={3}>
                    <Recipe
                        id={recipe.id}
                        title={recipe.name}
                        description={recipe.description}
                        image={recipe.imageUrl}
                    />
                </Col>
            ))}
        </Row>
    );
}

export default Browse;
